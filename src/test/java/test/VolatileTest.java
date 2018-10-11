package test;

public class VolatileTest {
	private boolean isLoad = false;

	private Object context = null;

	private void readContext() {
		isLoad = true;//模拟指令重排
		System.out.println("正在读取配置");
		context = new Object();
		//isLoad = true;指令重排前
	}

	private void initContext() {
		if (isLoad) {
			System.out.println("正在初始化");
			context.toString();
		} else {
			System.out.println("未读取配置");
		}
	}

	public static void main(String args[]) {
		VolatileTest test = new VolatileTest();
		Thread t1=new Thread(new Runnable() {
            @Override
            public void run() {
                test.readContext();
            }
        },"t1");
		
		Thread t2=new Thread(new Runnable() {
            @Override
            public void run() {

                    while (true){
                        if(test.isLoad){
                            test.initContext();
                            break;
                        }
                    }


            }
        },"t2");
		t1.start();
		t2.start();
	}
}
