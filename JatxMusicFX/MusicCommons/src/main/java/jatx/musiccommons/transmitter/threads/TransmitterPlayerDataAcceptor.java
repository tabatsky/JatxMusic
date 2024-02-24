package jatx.musiccommons.transmitter.threads;

public abstract class TransmitterPlayerDataAcceptor extends Thread {
    abstract void writeData(byte[] data);
}
