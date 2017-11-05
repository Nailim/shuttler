package com.oddrat.android.anemoi;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class AsyncWriter implements  Runnable {
	
    private final Writer out;
    private final BlockingQueue<Item> queue = new LinkedBlockingQueue<Item>();
    private volatile boolean started = false;
    private volatile boolean stopped = false;

    public AsyncWriter(File file) throws IOException {
        this.out = new BufferedWriter(new java.io.FileWriter(file));
    }
	
    public void open() {
        this.started = true;
        new Thread(this).start();
    }
    
    public void close() {
		this.stopped = true;
	}
    
	public void run() {
		while (!stopped) {
            try {
                Item item = queue.poll(1, TimeUnit.MILLISECONDS);
                if (item != null) {
                    try {
						item.write(out);
                    } catch (IOException logme) {
                    }
                }
            } catch (InterruptedException e) {
            }
        }
        try {
            out.close();
        } catch (IOException ignore) {
        }
		
	}

	public void append(CharSequence seq) {
		if (!started) {
            throw new IllegalStateException("open() call expected before append()");
        }
        try {
            queue.put(new CharSeqItem(seq));
        } catch (InterruptedException ignored) {
        }
	}

	

	private static interface Item {
        void write(Writer out) throws IOException;
    }

    private static class CharSeqItem implements Item {
        private final CharSequence sequence;

        public CharSeqItem(CharSequence sequence) {
            this.sequence = sequence;
        }

        public void write(Writer out) throws IOException {
            out.append(sequence);
        }
    }
}
