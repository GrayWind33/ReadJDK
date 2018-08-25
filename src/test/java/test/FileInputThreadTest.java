package test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileInputThreadTest implements Runnable {
	private int type;// 0做skip操作，1做读取操作

	private int gap;

	private FileInputStream in;

	public FileInputThreadTest(int type, FileInputStream in, int gap) {
		this.type = type;
		this.in = in;
		this.gap = gap;
	}

	@Override
	public void run() {
		byte[] body = new byte[gap];
		if (this.type == 0) {
			try {
				for (int i = 0; i < 4; i++) {
					in.skip(gap);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
				for (int i = 0; i < 5; i++) {
					in.read(body);
					System.out.println(Thread.currentThread().getName() + "-" + new String(body));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	public static void main(String args[]) throws IOException, InterruptedException {
		FileOutputStream out = new FileOutputStream("D:/test/file.txt");
		for (int i = 0; i < 1000; i++) {
			out.write("1234567890".getBytes());// 写入测试数据
		}
		out.close();
		FileInputStream in = new FileInputStream("D:/test/file.txt");
		FileInputThreadTest[] t = new FileInputThreadTest[10];
		Thread[] thread = new Thread[10];
		int i;
		for (i = 0; i < 10; i++) {
			t[i] = new FileInputThreadTest(1, in, 3);
			thread[i] = new Thread(t[i], "线程" + String.valueOf(i));
		}
		for (i = 0; i < 10; i++) {
			thread[i].start();
		}
		for (i = 0; i < 10; i++) {
			thread[i].join();
			;
		}
		in.close();
		/*
		 * 线程0-456 线程6-901 线程3-012 线程1-789 线程2-123 线程5-678 线程4-345 线程8-012 线程5-789
		 * 线程2-456 线程2-890 线程2-123 线程2-456 线程1-123 线程3-890 线程6-567 线程0-234 线程0-678
		 * 线程6-345 线程3-012 线程1-789 线程5-567 线程9-234 线程9-456 线程7-678 线程8-901 线程8-345
		 * 线程4-345 线程8-678 线程8-234 线程7-012 线程9-789 线程5-123 线程1-890 线程3-567 线程6-234
		 * 线程0-901 线程6-012 线程3-789 线程1-456 线程5-123 线程9-890 线程9-678 线程7-567 线程4-901
		 * 线程4-234 线程7-901 线程0-345 线程7-890 线程4-567
		 */
	}
}
