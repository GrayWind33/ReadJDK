package test;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class PipeStreamTest implements Runnable {
	private PipedInputStream in;
	private PipedOutputStream out;

	public PipeStreamTest(PipedInputStream in) {
		this.in = in;
	}

	public PipeStreamTest(PipedOutputStream out) {
		this.out = out;
	}

	@Override
	public void run() {
		if (this.in != null) {
			try {
				Thread.sleep(5000);// 这里阻塞了in，使得out会因没有空间写入而阻塞
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			boolean flag = true;
			while (flag) {
				try {
					int a = in.read();
					if (a == -1) {
						flag = false;
						break;
					}
					System.out.println(Thread.currentThread().getName() + " calculate " + String.valueOf(2 * a + 1));
				} catch (IOException e) {
					e.printStackTrace();
					flag = false;
				}
			}
		} else {
			try {
				Thread.sleep(1000);// 这里阻塞了out，导致in内没有可读取数据被阻塞
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			int a = 5;
			try {
				for (int i = 0; i < 15; i++) {
					out.write(a + i);
					System.out.println(Thread.currentThread().getName() + " write " + String.valueOf(a));
				}
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String args[]) throws IOException {
		PipedInputStream in = new PipedInputStream(10);
		PipedOutputStream out = new PipedOutputStream(in);
		new Thread(new PipeStreamTest(in)).start();
		new Thread(new PipeStreamTest(in)).start();
		new Thread(new PipeStreamTest(out)).start();
		/*Thread-0 calculate 11
		Thread-2 write 5
		Thread-2 write 5
		Thread-0 calculate 13
		Thread-2 write 5
		Thread-2 write 5
		Thread-2 write 5
		Thread-0 calculate 15
		Thread-0 calculate 19
		Thread-1 calculate 17*/
	}
}
