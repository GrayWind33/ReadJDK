package test;

import java.io.FileReader;
import java.io.IOException;

public class FileReaderTest {
	public static void main(String args[]) throws IOException{
		FileReader in = new FileReader("D:/test/file.txt");
		char[] buff = new char[10];
		in.read(buff);
		System.out.println(in.ready());
		System.out.println(buff);
	}
}
