package approaches.symbolic.api;

import game.rules.end.End;

import java.io.ByteArrayInputStream;

public class EndpointErrorHandling {
    static class ErrorClass extends Endpoint {
        @Override
        public String respond() {
            if (rawInput.equals("Meh Input")) {
                System.out.println("This is a print statement");
                System.out.println("This is a consecutive print statement");
                System.err.println("This is an error statement");
                System.out.println("This is separate print statement");
                System.err.println("This is separate error statement");
                System.err.println("This is consecutive error statement");
            }

            if (rawInput.equals("Bad Input")) {
                throw new RuntimeException("This is an exception");
            }

            return "This is a response";
        }
    }

    public static void main(String[] args) {
        System.setIn(new ByteArrayInputStream("Good Input\nMeh Input\nBad Input".getBytes()));
        new ErrorClass().start();
    }
}
