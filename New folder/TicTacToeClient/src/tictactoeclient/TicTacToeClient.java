
package tictactoeclient;

// Client side of client/server Tic-Tac-Toe program.
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.Socket;
import java.net.InetAddress;
import java.io.IOException;
import javax.swing.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public final class TicTacToeClient extends JFrame implements Runnable {

    private final JTextField idField; 
    private final JTextArea displayArea; 
    private final JPanel boardPanel;
    private final JPanel panel2;
    private final Square[][] board;
    private Square currentSquare; 
    private Socket connection; 
    private Scanner input;
    private Formatter output; 
    private final String ticTacToeHost; 
    private String myMark; 
    private boolean myTurn; 
    private final String X_MARK = "X"; 
    private final String O_MARK = "O";

    // set up user-interface and board
    public TicTacToeClient(String host) {
        ticTacToeHost = host; 
        displayArea = new JTextArea(4, 30); 
        displayArea.setEditable(false);
        add(new JScrollPane(displayArea), BorderLayout.SOUTH);

        boardPanel = new JPanel(); 
        boardPanel.setLayout(new GridLayout(3, 3, 0, 0));
        board = new Square[3][3];


        for (int row = 0; row < board.length; row++) {
            for (int column = 0; column < board[row].length; column++) {
                board[row][column] = new Square(" ", row * 3 + column);
                boardPanel.add(board[row][column]);       
            }
        }

        idField = new JTextField(); 
        idField.setEditable(false);
        add(idField, BorderLayout.NORTH);

        panel2 = new JPanel(); 
        panel2.add(boardPanel, BorderLayout.CENTER);
        add(panel2, BorderLayout.CENTER);

        setSize(300, 225); 
        setVisible(true); 

        startClient();
    }

    public void startClient() {

        try {

            connection = new Socket(InetAddress.getByName(ticTacToeHost), 2222);
            input = new Scanner(connection.getInputStream());
            output = new Formatter(connection.getOutputStream());
        } catch (IOException ioException) {
            System.out.println(ioException.toString());
        }

        ExecutorService worker = Executors.newFixedThreadPool(1);
        worker.execute(this);
    }

    @Override
    public void run() {
        myMark = input.nextLine();

        SwingUtilities.invokeLater(() -> {
            idField.setText("You are player \"" + myMark + "\"");
        });

        myTurn = (myMark.equals(X_MARK)); 
        while (true) {
            if (input.hasNextLine()) {
                processMessage(input.nextLine());
            }
        }
    }

    private void processMessage(String message) {
        switch (message) {
            case "Valid move.":
                displayMessage("Valid move, please wait.\n");
                setMark(currentSquare, myMark);
                break;
            case "Invalid move, try again":
                displayMessage(message + "\n"); 
                myTurn = true; 
                break;
            case "Opponent moved":
                int location = input.nextInt(); 
                input.nextLine();
                int row = location / 3; 
                int column = location % 3;
                setMark(board[row][column],
                        (myMark.equals(X_MARK) ? O_MARK : X_MARK));   
               displayMessage("Opponent moved. Your turn.\n");
                myTurn = true; 
                break;
            case "DEFEAT":
            case "TIE":
            case "VICTORY":

                displayMessage(message + "\n"); 
                myTurn = false;
                break;
            default:
                displayMessage(message + "\n");
                break;
        }
    }

    private void displayMessage(final String messageToDisplay) {
        SwingUtilities.invokeLater(() -> {
            displayArea.append(messageToDisplay); 
        });
    }


    private void setMark(final Square squareToMark, final String mark) {
        SwingUtilities.invokeLater(() -> {
            squareToMark.setMark(mark);
        });
    }


    public void sendClickedSquare(int location) {
        // if it is my turn
        if (myTurn) {
            output.format("%d\n", location); 
            output.flush();
            myTurn = false; 
        }
    }


    public void setCurrentSquare(Square square) {
        currentSquare = square; 
    }

    private class Square extends JPanel {

        private String mark; 
        private final int location; 

        public Square(String squareMark, int squareLocation) {
            mark = squareMark; 
            location = squareLocation; 

            addMouseListener(new MouseAdapter() {
                public void mouseReleased(MouseEvent e) {
                    setCurrentSquare(Square.this); 

                    // send location of this square
                    sendClickedSquare(getSquareLocation());
                }
            });
        }

       
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(30, 30);
        }

       
        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize(); 
        }

     
        public void setMark(String newMark) {
            mark = newMark; 
            repaint(); 
        }

       
        public int getSquareLocation() {
            return location; 
        }

       
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawRect(0, 0, 29, 29); 
            g.drawString(mark, 11, 20); 
        }
    }

}
