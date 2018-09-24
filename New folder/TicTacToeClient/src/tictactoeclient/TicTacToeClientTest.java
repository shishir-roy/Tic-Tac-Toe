
package tictactoeclient;

// Test class for Tic-Tac-Toe client.
import javax.swing.JFrame;
import java.util.*;

public class TicTacToeClientTest {

    public static void main(String[] args) {
        TicTacToeClient application;

        Scanner sc = new Scanner(System.in);
        String ip = sc.nextLine();
        if (args.length == 0) {
            application = new TicTacToeClient(ip); 
        } else {
            application = new TicTacToeClient(args[0]);
        }

        application.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
