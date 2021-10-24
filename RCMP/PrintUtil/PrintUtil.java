package PrintUtil;

// utility print functions to save my fingers and improve readability
class PrintUtil {
    
    public static void exception(Exception e, boolean debug) {
        if (debug)
            e.printStackTrace();
    }
    public static void debugln(String message, boolean debug) {
        if (debug)
            PrintUtil.println(message);

    }
    public static void debug(String message, boolean debug) {
        if (debug)
            PrintUtil.print(message);
    }
    public static void println(String message) {
        System.out.println(message);
        System.out.flush();
    }

    public static void print(String message) {
        System.out.print(message);
        System.out.flush();
    }
    
    public static void fault(String message) {
        System.err.println(message);
        System.err.flush();
        System.exit(0);
    }
}
