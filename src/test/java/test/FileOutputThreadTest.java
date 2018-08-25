package test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileOutputThreadTest implements Runnable {
	private byte[] txt;

	private FileOutputStream out;

	@Override
	public void run() {
		for (int i = 0; i < 10; i++) {
			try {
				out.write(txt);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public FileOutputThreadTest(String txt, FileOutputStream out) {
		this.txt = txt.getBytes();
		this.out = out;
	}

	public static void main(String args[]) throws InterruptedException, IOException {
		FileOutputStream out = new FileOutputStream("D:/test/file.txt");
		StringBuilder build = new StringBuilder();
		Thread[] t = new Thread[10];
		for (int i = 0; i < 10; i++) {
			build.setLength(0);
			for (int j = 0; j < 5; j++) {
				build.append(i);
			}
			t[i] = new Thread(new FileOutputThreadTest(build.toString(), out));
		}
		for (int i = 0; i < 10; i++) {
			t[i].start();
		}
		for (int i = 0; i < 10; i++) {
			t[i].join();
		}
		out.close();

		// 根据输出结果，每个字符应该连续出现5的倍数
		FileInputStream in = new FileInputStream("D:/test/file.txt");
		int pos = 0;
		while (in.available() > 0) {
			int text = in.read();
			pos++;
			for (int i = 1; i < 5; i++) {
				if (text != in.read()) {
					System.out.println("error" + String.valueOf(pos));// 没有出现
					return;
				}
				pos++;
			}
		}
		/*
		 * 00000000000000000000000000000000000000000000000000
		 * 22222222222222211111222222222222222222222222222222输入有交错
		 * 22222111111111111111111111111111111111111111111111
		 * 33333333333333333333333333333333333333333333333333
		 * 55555555555555555555555555555555555555555555555555
		 * 44444444444444444444444444444444444444444444444444
		 * 66666666666666666666666666666666666666666666666666
		 * 99999999999999999999999998888888888777777777777777
		 * 77777777777777777777777777777777777999999999999999
		 * 99999999998888888888888888888888888888888888888888
		 */
	}

}
