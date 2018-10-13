package test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class CallableTest implements Callable<Integer>{
	private int start;
	
	public CallableTest(int start) {
		this.start = start;
	}
	
	@Override
	public Integer call() throws Exception {
		Thread.sleep(500);
		return start + 1;
	}
	
	public static void main(String args[]) throws InterruptedException, ExecutionException{
		long start = System.currentTimeMillis();
		FutureTask<Integer> task1 = new FutureTask<>(new CallableTest(2));
		new Thread(task1).start();
		FutureTask<Integer> task2 = new FutureTask<>(new CallableTest(4));
		new Thread(task2).start();
		System.out.println(task1.get() + task2.get());//8
		long end = System.currentTimeMillis();
		System.out.println(end - start);//506
	}
}
