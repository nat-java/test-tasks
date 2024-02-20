public class Main {
    public static void main(String[] args) {
        HandlerImpl handler = new HandlerImpl();
        System.out.println(handler.performOperation("1"));
        System.out.println(handler.performOperation("2"));
        System.out.println(handler.performOperation("3"));
        System.out.println(handler.performOperation("4"));
    }
}
