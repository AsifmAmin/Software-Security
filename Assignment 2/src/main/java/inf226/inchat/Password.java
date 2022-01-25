package inf226.inchat;
import com.lambdaworks.crypto.SCryptUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public final class Password {
    public final String pwd;

    public Password(String pwd) {
        this.pwd = pwd;
    }
    public static Password create(String password) {
        int N = 16384;
        int r = 8;
        int p = 1;
        String pwd = SCryptUtil.scrypt(password,N,r,p);
        return new Password(pwd);
    }

    public static boolean verify(String password){
        System.out.println ("fml");
        return  !(notEmpty(password)) && lenghtChecker(password) && !(PasswordList ( password ));
    }


    public static  boolean lenghtChecker(String password){
        return password.length () > 8;
    }
    public static boolean notEmpty(String password){
        return password.isEmpty ();
    }
    //Check for common password from leaked databases
    public static boolean PasswordList(String password){

        ArrayList<String> pass = new ArrayList<> ();
        String line = null;

        try{
            FileReader fr= new FileReader("10-million-password-list-top-10000.txt");
            BufferedReader buffer = new BufferedReader(fr);
            while ((line = buffer.readLine()) != null){
                pass.add(line);
            }

            buffer.close ();

        } catch (IOException e) {
            e.printStackTrace ();
        }

        return pass.contains (password);
    }

    public String toString(){

        return this.pwd;
    }

}
