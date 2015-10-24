package co.nordlander.a;

/**
 *      Need to grab stdout somehow during tests.
 *      This class makes that possible.
 */
public class ATestOutput implements AOutput{

    private static final String LN = System.getProperty("line.separator");

    StringBuffer sb = new StringBuffer();
    public void output(Object... args) {
        for(Object arg : args){
            sb.append(arg.toString());
            System.out.print(arg.toString());
        }
        sb.append(LN);
        System.out.println("");
    }

    public String grab(){
        String ret = sb.toString();
        sb = new StringBuffer();
        return ret;
    }

    public String get(){
        return sb.toString();
    }
}