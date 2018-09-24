
package tictactoeserver;

// Server side of client/server Tic-Tac-Toe program.
import java.awt.BorderLayout;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;

public class TicTacToeServer extends JFrame {

    private final String[] board = new String[9]; 
    private final JTextArea outputArea; 
    private final Player[] players;
    private ServerSocket server; 
    private int currentPlayer; 
    private final static int PLAYER_X = 0; 
    private final static int PLAYER_O = 1; 
    private final static String[] MARKS = {"X", "O"}; 
    private final ExecutorService runGame; 
    private final Lock gameLock; 
    private final Condition otherPlayerConnected; 
    private final Condition otherPlayerTurn; 

    public TicTacToeServer() {
        super("Tic-Tac-Toe Server"); 

        runGame = Executors.newFixedThreadPool(2);
        gameLock = new ReentrantLock(); 


        otherPlayerConnected = gameLock.newCondition();

   
        otherPlayerTurn = gameLock.newCondition();

        for (int i = 0; i < 9; i++) {
            board[i] = ""; 
        }
        players = new Player[2]; 
        currentPlayer = PLAYER_X; 

        try {
            server = new ServerSocket(2222, 2); 
        } catch (IOException ioException) {
            System.out.println(ioException.toString());
            System.exit(1);
        }

        outputArea = new JTextArea(); 
        add(outputArea, BorderLayout.CENTER);
        outputArea.setText("Server awaiting connections\n");
        setSize(300, 300);
        setVisible(true); 
    }

    public void execute() {
        for (int i = 0; i < players.length; i++) {
            try {
                
                players[i] = new Player(server.accept(), i);
                runGame.execute(players[i]);
            } catch (IOException ioException) {
                System.out.println(ioException.toString());
                System.exit(1);
            }
        }

        gameLock.lock(); 

        try {
            players[PLAYER_X].setSuspended(false); 
            otherPlayerConnected.signal(); 
        } finally {
            gameLock.unlock(); 
        }
    }

    private void displayMessage(final String messageToDisplay) {
        SwingUtilities.invokeLater(() -> {
            outputArea.append(messageToDisplay);
        });
    }

    public boolean validateAndMove(int location, int player) throws InterruptedException {
        while (player != currentPlayer) {
            gameLock.lock();

            otherPlayerTurn.await(); 
            gameLock.unlock();
        }

        if (!isOccupied(location)) {
            board[location] = MARKS[currentPlayer]; 
            currentPlayer = (currentPlayer + 1) % 2;
            players[currentPlayer].otherPlayerMoved(location);

            gameLock.lock(); 

            try {
                otherPlayerTurn.signal(); 
            } finally {
                gameLock.unlock();
            }

            return true; 
        } else {
            return false; 
        }
    }

    public boolean isOccupied(int location) {
        return board[location].equals(MARKS[PLAYER_X]) || board[location].equals(MARKS[PLAYER_O]);
    }

    public boolean hasWinner() {
        return (!board[0].isEmpty() && board[0].equals(board[1]) && board[0].equals(board[2]))
                || (!board[3].isEmpty() && board[3].equals(board[4]) && board[3].equals(board[5]))
                || (!board[6].isEmpty() && board[6].equals(board[7]) && board[6].equals(board[8]))
                || (!board[0].isEmpty() && board[0].equals(board[3]) && board[0].equals(board[6]))
                || (!board[1].isEmpty() && board[1].equals(board[4]) && board[1].equals(board[7]))
                || (!board[2].isEmpty() && board[2].equals(board[5]) && board[2].equals(board[8]))
                || (!board[0].isEmpty() && board[0].equals(board[4]) && board[0].equals(board[8]))
                || (!board[2].isEmpty() && board[2].equals(board[4]) && board[2].equals(board[6]));
    }


    public boolean boardFilledUp() {
        for (int i = 0; i < board.length; ++i) {
            if (board[i].isEmpty()) {
                return false;
            }
        }

        return true;
    }

    public boolean isGameOver() {
        return hasWinner() || boardFilledUp();
    }

    private class Player implements Runnable {

        private final Socket connection;
        private Scanner input;
        private Formatter output;
        private final int playerNumber; 
        private final String mark;
        private boolean suspended = true; 

        public Player(Socket socket, int number) {
            playerNumber = number; 
            mark = MARKS[playerNumber]; 
            connection = socket;

            try {
                input = new Scanner(connection.getInputStream());
                output = new Formatter(connection.getOutputStream());
            } catch (IOException ioException) {
                System.out.println(ioException.toString());
                System.exit(1);
            }
        }


        public void otherPlayerMoved(int location) {
            output.format("Opponent moved\n");
            output.format("%d\n", location); 
            output.flush();
            output.format(hasWinner() ? "DEFEAT\n" : boardFilledUp() ? "TIE\n" : "");
            output.flush();
        }

        @Override
        public void run() {
            try {
                displayMessage("Player " + mark + " connected\n");
                output.format("%s\n", mark); 
                output.flush(); 
                if (playerNumber == PLAYER_X) {
                    output.format("%s\n%s", "Player X connected", "Waiting for another player\n");
                    output.flush(); 
                    gameLock.lock();

                    try {
                        while (suspended) {
                            otherPlayerConnected.await(); 
                        }
                    } catch (InterruptedException exception) {
                        System.out.println(exception.toString());
                    } finally {
                        gameLock.unlock(); 
                    }

                    output.format("Other player connected. Your move.\n");
                    output.flush(); 
                } else {
                    output.format("Player O connected, please wait\n");
                    output.flush();
                }

                while (!isGameOver()) {
                    int location = 0; 

                    if (input.hasNext()) {
                        location = input.nextInt(); 
                    }
                    if (validateAndMove(location, playerNumber)) {
                        displayMessage("\nlocation: " + location);
                        output.format("Valid move.\n"); 
                        output.flush(); 
                        output.format(hasWinner() ? "VICTORY\n" : boardFilledUp() ? "TIE\n" : "");
                        output.flush();
                    } else {
                      
                        output.format("Invalid move, try again\n");
                        output.flush(); 
                    }
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(TicTacToeServer.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    connection.close(); 
                } catch (IOException ioException) {
                    System.out.println(ioException.toString());
                    System.exit(1);
                }
            }
        }

        public void setSuspended(boolean status) {
            suspended = status;
        }
    }

}
