package com.focusit.jsflight.recorder.internalevent;

import java.io.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPOutputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.FastOutput;

/**
 * Recorder for special server event that must be recorded to get correct overall recording.
 * For example if you record only user interaction and miss same important server state changes,
 * so it might get very hard to replay that recording
 * <p>
 * Created by doki on 30.07.16.
 */
public class InternalEventRecorder
{
    private static final InternalEventRecorder instance = new InternalEventRecorder();
    private final int maxElementsBeforeFlush;
    private final boolean storeInGzip;
    private ArrayBlockingQueue<InternalEventRecord> records;
    private AtomicLong lastId = new AtomicLong(-1);
    private AtomicLong timestampNs = new AtomicLong(0);
    private AtomicBoolean recording = new AtomicBoolean(false);
    private volatile String eventsFilename = "internal.data";
    private StorageThread storageThread;
    private AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private AtomicBoolean newFile = new AtomicBoolean(false);
    private WallClock wallClock = new WallClock();

    private InternalEventRecorder()
    {
        this(-1, 4096, "internal-event-storage", false);
    }

    private InternalEventRecorder(int maxElementsBeforeFlush, int maxQueueSize, String storagePrefix,
            boolean storeInGzip)
    {
        this.maxElementsBeforeFlush = maxElementsBeforeFlush;
        this.records = new ArrayBlockingQueue<>(maxQueueSize);
        this.storeInGzip = storeInGzip;
        this.storageThread = new StorageThread(storagePrefix);
        wallClock.start();
        storageThread.start();
    }

    public static InternalEventRecorder getInstance()
    {
        return instance;
    }

    public static InternalEventRecorder build(int maxElementsBeforeFlush, int maxQueueSize, String storagePrefix,
            boolean storeInGzip)
    {
        return new InternalEventRecorder(maxElementsBeforeFlush, maxQueueSize, storagePrefix, storeInGzip);
    }

    public long getWallTime()
    {
        return timestampNs.get();
    }

    public void openFileForWriting(String filename)
    {
        this.eventsFilename = filename;
        storageThread.openFileForWriting();
    }

    public void push(String tag, Object data) throws UnsupportedEncodingException, InterruptedException
    {
        if (!recording.get())
        {
            return;
        }

        InternalEventRecord record = new InternalEventRecord();
        record.id = lastId.incrementAndGet();

        String tagValue = tag.trim();
        if (!tagValue.isEmpty())
        {
            tagValue.getChars(0, tagValue.length() > 64 ? 64 : tagValue.length(), record.tag, 0);
        }
        if (data != null)
        {
            record.data = data;
        }
        record.timestampNs = timestampNs.get();
        records.put(record);
    }

    public void shutdown() throws InterruptedException
    {
        shuttingDown.set(true);
        storageThread.join(2000);
        wallClock.join(2000);
        storageThread.flush();
    }

    public void recordToNewFile()
    {
        storageThread.toNewFile();
    }

    public void startRecording()
    {
        recording.set(true);
    }

    public void stopRecording()
    {
        recording.set(false);
    }

    public void setWallClockInterval(long interval)
    {
        this.wallClock.setNewInterval(interval);
    }

    /**
     * Internal event representation
     */
    public static class InternalEventRecord
    {
        public long id;
        public long timestampNs;
        public char tag[] = new char[64];
        public Object data;
    }

    class StorageThread extends Thread
    {
        private Kryo kryo;
        private FastOutput output;

        private int filesCounter = 1;

        public StorageThread(String storagePrefix)
        {
            super(storagePrefix);
            setPriority(NORM_PRIORITY);
            kryo = new Kryo();
        }

        public void flush()
        {
            output.flush();
        }

        public void openFileForWriting()
        {
            openFile(eventsFilename);
        }

        private void openFile(String fileName)
        {
            try
            {
                File destinationFile = new File(fileName);
                System.out.println("Storing internal events to " + destinationFile.getAbsolutePath());
                FileOutputStream fos = new FileOutputStream(fileName);
                OutputStream out = storeInGzip ? new GZIPOutputStream(fos) : fos;
                output = new FastOutput(out);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        public void toNewFile()
        {
            newFile.set(true);
        }

        private void reOpenFile()
        {
            try
            {
                output.close();
                openFile(eventsFilename + filesCounter++);
            }
            finally
            {
                newFile.set(false);
            }
        }

        @Override
        public void run()
        {
            boolean needFlush = false;
            int flushedObjects = 0;
            while (isAlive() && !isInterrupted() && !shuttingDown.get())
            {
                try
                {
                    if (recording.get())
                    {
                        InternalEventRecord record;
                        if (flushedObjects != maxElementsBeforeFlush && (record = records.poll()) != null)
                        {
                            kryo.writeObject(output, record);
                            needFlush = true;
                            flushedObjects++;
                            yield();
                        }
                        else
                        {
                            if (needFlush)
                            {
                                output.flush();
                                flushedObjects = 0;
                                needFlush = false;
                            }
                            if (newFile.get())
                            {
                                reOpenFile();
                            }
                            sleep(100);
                        }
                    }
                    else
                    {
                        sleep(1000);
                    }
                }
                catch (Exception e)
                {
                    // no Exception could break this thread. only Error
                }
            }
        }
    }

    class WallClock extends Thread
    {
        //defaults to 10 milliseconds
        private long interval = 10;

        public WallClock()
        {
            super("internal-event-clock");
            setPriority(MAX_PRIORITY);
        }

        public void setNewInterval(long interval)
        {
            this.interval = interval;
        }

        @Override
        public void run()
        {
            while (isAlive() && !isInterrupted() && !shuttingDown.get())
            {
                timestampNs.set(System.nanoTime());
                try
                {
                    if (interval <= 0)
                    {
                        Thread.yield();
                    }
                    else
                    {
                        Thread.sleep(interval);
                    }
                }
                catch (InterruptedException e)
                {
                    break;
                }
            }
        }
    }
}
