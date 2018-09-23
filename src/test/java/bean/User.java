package bean;

import java.io.Externalizable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class User implements Externalizable {
	private static final long serialVersionUID = 1L;

	private String name;
	private boolean isMale;
	private int age;
	private char[] description;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isMale() {
		return isMale;
	}

	public void setMale(boolean isMale) {
		this.isMale = isMale;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public char[] getDescription() {
		return description;
	}

	public void setDescription(char[] description) {
		this.description = description;
	}

	public String toString() {
		String res = name;
		if (isMale) {
			res += ", male";
		} else {
			res += ", female";
		}
		res += " " + String.valueOf(age) + ", ";
		res += new String(description);
		return res;
	}
	
    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException{
            s.defaultWriteObject();
        }

	public static void main(String args[]) throws IOException {
		try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("D:/test/file.txt"))) {
			User user = new User();
			user.setAge(20);
			user.setMale(true);
			user.setName("Tony");
			char[] description = { 'a', 'b', 'c', '1' };
			user.setDescription(description);
			System.out.println(user);
			out.writeObject(user);
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeUTF(name);
		out.writeInt(age);
		out.writeBoolean(isMale);
		out.writeChars(new String(description));
		
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		// TODO Auto-generated method stub
		
	}
}
