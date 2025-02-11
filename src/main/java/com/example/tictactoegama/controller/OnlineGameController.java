package com.example.tictactoegama.controller;

import com.example.tictactoegama.Api.Client;
import com.example.tictactoegama.Api.ClientHandler;
import com.example.tictactoegama.Api.RequestHandler;
import com.example.tictactoegama.interfaces.EndGame;
import com.example.tictactoegama.models.PlayBoard;
import com.example.tictactoegama.models.Player;
import com.example.tictactoegama.models.VideoViewHandler;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ResourceBundle;

public class OnlineGameController implements Initializable, EndGame {
    private static boolean flag;
    @FXML
    Button replayBtn;
    @FXML
    private Text gameStatus;
    @FXML
    private GridPane gameGrid;
    public static String currentPlayer;
    @FXML
    private Line winnerLine;
    @FXML
    Label playerXNametxt;
    static String playerx;
    @FXML
    Label playerONametxt;
    static String playero;
    @FXML
    Label OScoreLabel;
    static String playerOscore;
    @FXML
    Label XScoreLabel;
    static String playerXscore;
    private static String opponentSymbol;
    private PlayBoard playBoard;
    private boolean gameEnded;
    private VideoViewHandler videoViewHandler;
    public static Player opponent;
    Thread th;

    public synchronized static void setPlayers(boolean flag,Player user, Player opponent){
        OnlineGameController.flag = !flag;
        if (OnlineGameController.flag) {
            OnlineGameController.opponent = user;
            playerOscore = ""+ user.getScore();
            playerXscore= "" + opponent.getScore();
            playerx = opponent.getUsername();
            playero = user.getUsername();
            currentPlayer = "O";
            opponentSymbol = "X";
        }
        else {
            OnlineGameController.opponent = opponent;
            playerOscore = ""+ opponent.getScore();
            playerXscore= "" + user.getScore();
            playerx = user.getUsername();
            playero = opponent.getUsername();
            currentPlayer = "X";
            opponentSymbol = "O";
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        playBoard = new PlayBoard();
        gameEnded = false;
        videoViewHandler = new VideoViewHandler();
        Platform.runLater(()->{
        XScoreLabel.setText(playerXscore);
        playerXNametxt.setText(playerx);
        OScoreLabel.setText(playerOscore);
        playerONametxt.setText(playero);
        gameStatus.setText(playerx);
        if(flag){
            gameStatus.setText(playero);
            disableButtons();
            System.out.println("not my turn");
            processComputerMove();
            System.out.println("listening");
        }
        });
    }

    @FXML
    private void handleButtonClick(ActionEvent event) {
        if (gameEnded) {
            Platform.runLater(()->disableButtons());
        }
        Button clickedButton = (Button) event.getSource();
        if (clickedButton.getText().isEmpty()) {
            int row = GridPane.getRowIndex(clickedButton);
            int col = GridPane.getColumnIndex(clickedButton);
            try {
                processPlayerMove(clickedButton, row, col);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }

    private void processPlayerMove(Button clickedButton, int row, int col) throws IOException {
        clickedButton.setText(currentPlayer);
        updateButtonStyle(clickedButton, currentPlayer);
        int index = row*3+col;
        ClientHandler.send("{\"RequestType\":\"InGame\", \"value\":"+index+", \"Player\":"+opponent+"}");
        Platform.runLater(()-> {
            updateBoardUI();
            disableButtons();
        });
        int flag = playBoard.play(row, col, currentPlayer.charAt(0));
        if (flag == 1) {
            endGame(currentPlayer);
        }
        if (flag == 0) {
            endGame("draw");
        }
        if (!gameEnded) {
            gameStatus.setText(playero);
            processComputerMove();
        }
    }

    private void processComputerMove() {
            th = new Thread(()->{
                int i;
                i=RequestHandler.getPlay();
            while((i= RequestHandler.getPlay())<0) {
            }
                int flag = playBoard.play(i, opponentSymbol.charAt(0));
                if (flag == 1) {
                    endGame(opponentSymbol);
                } else if (flag == 0) {
                    endGame("draw");
                } else if (flag == 10) {
                    endGame(currentPlayer);
                }
                updateBoardUI();
                if (!gameEnded) {
                    Platform.runLater(() -> {
                        enableButtons();
                        gameStatus.setText(playerx);
                    });
                }
                if (gameEnded){
                    th.stop();
                }
            });
            th.start();
            if(!th.isInterrupted() && !gameEnded){
                RequestHandler.index = -1;
                th.resume();
            }
    }

    private void updateButtonStyle(Button button, String symbol) {
        if ("X".equals(symbol)) {
            button.setStyle(
                    "-fx-background-color: rgba(70, 163, 255, 0.2); " +
                            "-fx-text-fill: #46A3FF; " +
                            "-fx-font-size: 36px; " +
                            "-fx-border-color: #E3E3E3;"
            );
        } else if ("O".equals(symbol)) {
            button.setStyle(
                    "-fx-background-color: rgba(255, 130, 126, 0.2); " +
                            "-fx-text-fill: #FF827E; " +
                            "-fx-font-size: 36px; " +
                            "-fx-border-color: #E3E3E3;"
            );
        }
    }

    private void updateBoardUI() {
        Platform.runLater(()->{
            char[][] board = playBoard.getBoard();
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    char symbol = board[i][j];
                    if (symbol != '_') {
                        Button button = (Button) getNodeByRowColumnIndex(i, j);
                        if (button != null) {
                            button.setText(String.valueOf(symbol));
                            updateButtonStyle(button, String.valueOf(symbol));
                        }
                    }
                }
            }
        });
    }

    public void endGame(String winner) {
        ClientHandler.send("{\"RequestType\":\"GameEnded\",\"Player\":"+opponent+"}");
            String videoPath = "";
            if (winner.equals(currentPlayer)) {
                videoPath = "src/main/resources/com/example/tictactoegama/videos/video_win.mp4";

            } else if (winner.equals(opponentSymbol)) {
                videoPath = "src/main/resources/com/example/tictactoegama/videos/video_fail.mp4";
            } else if (winner.equals("draw")) {
                videoPath =
                        "src/main/resources/com/example/tictactoegama/videos/video_draw1.mp4";
            } else {
                return;
            }
            disableButtons();
            gameEnded = true;
            replayBtn.setVisible(true);
            final String finalVideoPath = videoPath;
            PauseTransition delay = new PauseTransition(Duration.seconds(1));
            delay.setOnFinished(event -> {
                Stage stage = new Stage();
                videoViewHandler.showVideoView(stage, finalVideoPath);
                stage.show();
            });
            delay.play();
            if (playBoard.getWinningTiles() != null ) {
                drawWinnerLine(playBoard.getWinningTiles());
                winnerLine.setVisible(true);
            }
    }

    private void disableButtons() {
        Platform.runLater(() -> {
            for (Node node : gameGrid.getChildren()) {
                if (node instanceof Button) {
                    node.setDisable(true);
                }
            }
        });
    }

    private void enableButtons() {
        Platform.runLater(() -> {
            for (Node node : gameGrid.getChildren()) {
                if (node instanceof Button) {
                    node.setDisable(false);
                }
            }
        });
    }

    private void drawWinnerLine(int[][] winningTiles) {
        if (winningTiles == null || winningTiles.length == 0) return;

        // Get the starting and ending nodes of the winning line
        Node startNode = getNodeByRowColumnIndex(winningTiles[0][0], winningTiles[0][1]);
        Node endNode = getNodeByRowColumnIndex(winningTiles[2][0], winningTiles[2][1]);

        if (startNode != null && endNode != null) {
            // Get the position of the GridPane
            Bounds gridPaneBounds = gameGrid.localToScene(gameGrid.getBoundsInLocal());

            // Calculate the center points of the start and end nodes relative to the GridPane
            double startX = startNode.getBoundsInParent().getMinX() + startNode.getBoundsInParent().getWidth() / 2;
            double startY = startNode.getBoundsInParent().getMinY() + startNode.getBoundsInParent().getHeight() / 2;
            double endX = endNode.getBoundsInParent().getMinX() + endNode.getBoundsInParent().getWidth() / 2;
            double endY = endNode.getBoundsInParent().getMinY() + endNode.getBoundsInParent().getHeight() / 2;

            // Adjust the winnerLine coordinates relative to the GridPane
            winnerLine.setStartX(gridPaneBounds.getMinX() + startX);
            winnerLine.setStartY(gridPaneBounds.getMinY() + startY);
            winnerLine.setEndX(gridPaneBounds.getMinX() + endX);
            winnerLine.setEndY(gridPaneBounds.getMinY() + endY);
            winnerLine.setVisible(true);
        }
    }

    private Node getNodeByRowColumnIndex(final int row, final int column) {
        for (Node node : gameGrid.getChildren()) {
            if (GridPane.getRowIndex(node) != null && GridPane.getRowIndex(node) == row &&
                    GridPane.getColumnIndex(node) != null && GridPane.getColumnIndex(node) == column) {
                return node;
            }
        }
        return null;
    }

    @FXML
    public void handleReplay(ActionEvent event) throws IOException {
        ClientHandler.send("{\"RequestType\":\"RequestGame\",\"userid\":"+Client.user.getUserid()+",\"opponentid\":"+OnlineGameController.opponent.getUserid()+"}");
        replayBtn.setDisable(true);
    }

    @FXML
    public void handleGoBack(ActionEvent event) throws IOException {
        if (!gameEnded){
            Parent optionPageParent = FXMLLoader.load(getClass().getResource("/com/example/tictactoegama/views/surrender.fxml"));
            Scene optionPageScene = new Scene(optionPageParent);
            Stage window = new Stage();
            window.setScene(optionPageScene);
            window.initModality(Modality.APPLICATION_MODAL);
            window.show();
        }
        else {
            Parent optionPageParent = FXMLLoader.load(getClass().getResource("/com/example/tictactoegama/views/ListOfAvailablePlayers.fxml"));
            Scene optionPageScene = new Scene(optionPageParent);
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(optionPageScene);
            window.show();
        }
    }
}
