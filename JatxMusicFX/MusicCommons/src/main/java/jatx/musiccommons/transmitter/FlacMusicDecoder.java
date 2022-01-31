package jatx.musiccommons.transmitter;

import io.nayuki.flac.common.StreamInfo;
import io.nayuki.flac.decode.FlacDecoder;
import jatx.musiccommons.util.Frame;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import static jatx.musiccommons.util.Frame.FRAME_RATES;

public class FlacMusicDecoder extends MusicDecoder {
    private FlacDecoder decoder;
    private StreamInfo streamInfo;

    private File mCurrentFile = null;

    private float msFrame = 0f;
    private int freq = 0;
    private int channels = 0;
    private int depth = 0;
    private long numSamples = 0;

    @Override
    public void setFile(File f) throws MusicDecoderException {
        if (f == null || !f.exists()) {
            throw new MusicDecoderException("File Read Error");
        }

        mCurrentFile = f;

        try {
            AudioFile af;
            af = AudioFileIO.read(f);

            trackLengthSec = af.getAudioHeader().getTrackLength();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            decoder = new FlacDecoder(f);
            while (decoder.readAndHandleMetadataBlock() != null){
            }
            streamInfo = decoder.streamInfo;
            numSamples = streamInfo.numSamples;
            if (numSamples == 0L) {
                throw new MusicDecoderException("Unknown audio length");
            }

            System.out.println("sampleRate: " + streamInfo.sampleRate);
            System.out.println("sampleDepth: " + streamInfo.sampleDepth);
            System.out.println("numChannels: " + streamInfo.numChannels);
        } catch (Exception e) {
            e.printStackTrace();
            throw new MusicDecoderException("File Read Error");
        }

        currentMs = 0;
        resetTimeFlag = true;

        System.out.println("set file: " + f.getName());
    }

    @Override
    public synchronized Frame readFrame() throws MusicDecoderException, Frame.WrongFrameException, TrackFinishException {
        int position = sPosition;

        if (streamInfo == null) {
            throw new MusicDecoderException("streamInfo is null");
        }

        freq = streamInfo.sampleRate;
        channels = streamInfo.numChannels;
        depth = streamInfo.sampleDepth;

        boolean wrongRate = true;
        for (int rate: FRAME_RATES) {
            if (rate == freq) wrongRate = false;
        }
        if (wrongRate) throw new Frame.WrongFrameException("(player) wrong frame rate: $freq");

        int bytesPerSample = depth / 8;
        ByteArrayOutputStream outStream = new ByteArrayOutputStream(65536 * channels * bytesPerSample);

        try {
            int [][] samples = new int[channels][65536];

            int blockSamples = decoder.readAudioBlock(samples, 0);

            if (channels == 2) {
                for (int i = 0; i < blockSamples; i++) {
                    for (int ch = 0; ch < channels; ch++) {
                        int value = samples[ch][i];
                        if (bytesPerSample == 4) {
                            int j = 0;
                            int value16 = value >> 16;
                            while (j < 2) {
                                outStream.write(value16 >> (j * 8) & 0xFF);
                                j++;
                            }
                        } else if (bytesPerSample == 3) {
                            int j = 0;
                            int value16 = value >> 8;
                            while (j < 2) {
                                outStream.write(value16 >> (j * 8) & 0xFF);
                                j++;
                            }
                        } else {
                            int j = 0;
                            while (j < bytesPerSample) {
                                outStream.write(value >> (j * 8) & 0xFF);
                                j++;
                            }
                        }
                    }
                }

                msFrame = blockSamples * 1e3f / freq;
                msRead += msFrame;
                msTotal += msFrame;
                currentMs += msFrame;

                if (msFrame == 0f) {
                    throw new TrackFinishException();
                }
            } else if (channels == 1) {
                throw new Frame.WrongFrameException("(player) mono sound");
            } else {
                throw new Frame.WrongFrameException("(player) $channels channels");
            }
        } catch (IOException e) {
            throw new Frame.WrongFrameException("IOException");
        }

        byte[] data = outStream.toByteArray();
        Frame frame = new Frame();
        frame.size = data.length;
        frame.freq = freq;
        frame.channels = channels;
        frame.depth = depth;
        frame.position = position;
        frame.data = data;
        return frame;
    }

    @Override
    public synchronized void seek(double progress) throws MusicDecoderException {
        int[][] samples = new int[channels][65536];
        long seekPosition = (long) (numSamples * progress);
        try {
            decoder.seekAndReadAudioBlock(seekPosition, samples, 0);
            msRead = msFrame * (int) (trackLengthSec * 1000f * progress / msFrame);
            currentMs = msFrame * (int) (trackLengthSec * 1000f * progress / msFrame);
        } catch (IOException e) {
            throw new MusicDecoderException("IOException");
        }
    }
}
