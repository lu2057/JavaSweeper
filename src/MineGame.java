import java.util.Random;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

public class MineGame {
    //utilities:
    Random rand = new Random();

    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    int width = screen.width;

    // prepare a reusable GridBagConstraints
    GridBagConstraints gridConstraints = new GridBagConstraints();
    GridBagConstraints choiceConstraints = new GridBagConstraints();
    GridBagConstraints difficultyConstraints = new GridBagConstraints();
    GridBagConstraints textConstraints = new GridBagConstraints();
    
    //Cell class:
    private class Cell{
        //cell state:
        private boolean isMine;
        private boolean isRevealed;
        private boolean isFlagged;
        private int adjacentMines;

        private Cell(){
            
            //default state:
            this.isMine = false;
            this.isRevealed = false;
            this.isFlagged = false;
            this.adjacentMines = 0;
        }

        //cell methods:
        private boolean isMine(){ //check if cell is a mine
            return this.isMine;
        }
        private void setMine(){ //set cell as mine
            this.isMine = true;
            this.adjacentMines=0;
        }

        private boolean isRevealed(){ //check if cell is revealed
            return this.isRevealed;
        }
        private void reveal(){ //reveal cell
            this.isRevealed = true;
        }

        private boolean isFlagged(){ //check if cell is flagged
            return this.isFlagged;
        }
        private void toggleFlag(){ //toggle flag on cell
            this.isFlagged = !this.isFlagged;
        }

        private int getAdjacentMines(){ //get number of adjacent mines
            return this.adjacentMines;
        }
        private void incrementAdjacentMines(){
            this.adjacentMines++;
        }

        //override toString() for debugging
        @Override
        public String toString(){
            if(this.isMine){
                return "B";
            }
            return Integer.toString(this.adjacentMines);
        }
    }

    //gamestate:
    Cell[][] cellBoard; //2D array of cells
    int rows;
    int cols;
    
    int totalMines;
    final int safeRadius = 2; //radius around first click that is guaranteed to be safe
    int correctFlags;
    int incorrectFlags;
    int revealedSafeCells;

    boolean firstClick; //to check if first click has been made
    boolean mode; //false = reveal mode, true = flag mode

    boolean hasWon;

    //GUI elements:
    
    JFrame frame = new JFrame("MineGame");
    JPanel mainPanel = new JPanel();

    JLabel textLabel = new JLabel(); //displays number of mines, win/lose messages
    JPanel textPanel = new JPanel();
    
    JPanel gridPanel = new JPanel();
    JButton[][] buttons; //2D array of buttons for the cells
    int buttonsize; //size of each button in pixels
    
    
    JPanel choicePanel = new JPanel();
    JButton[] choiceButtons = new JButton[2]; //0 = reveal mode, 1 = flag mode
    
    JPanel difficultyPanel= new JPanel();
    //sliders for custom difficulty
    JSlider mineSlider = new JSlider();
    JLabel mineLabel = new JLabel();
    JSlider rowSlider = new JSlider(); 
    JLabel rowLabel = new JLabel();
    JSlider colSlider = new JSlider(); 
    JLabel colLabel = new JLabel();
    JButton startButton = new JButton("Start Game");
    
    JButton newGameButton= new JButton("New Game");
    JButton resetButton = new JButton("Start Over"); //resets the game (new mine placement with same difficulty)
    


    Color[] colors = {new Color(0,0,255), new Color(0,128,0), new Color(255,0,0), new Color(0,0,128),
                      new Color(128,0,0), new Color(0,128,128), new Color(0,0,0), new Color(128,128,128)};
                      //1                   2                   3                   4      
                      //5                   6                   7                   8

    ImageIcon flagIcon = new ImageIcon(getClass().getResource("/lib/flag.png"));
    ImageIcon mineIcon = new ImageIcon(getClass().getResource("/lib/mine.png"));

    public MineGame(){
        //FRAME SETUP
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());
        mainPanel.setLayout(new java.awt.GridBagLayout()); //main panel uses GridBagLayout for flexible arrangement
        frame.add(mainPanel);
        
        //CONSTRAINTS SETUP

        gridConstraints.gridx = 0;
        gridConstraints.anchor = GridBagConstraints.CENTER;
        gridConstraints.insets = new Insets(10,10,10,10); 


        difficultyConstraints.gridx = 0;
        difficultyConstraints.anchor = GridBagConstraints.CENTER;
        difficultyConstraints.insets = new Insets(10,10,10,10); 

        //TEXT PANEL SETUP
        textLabel.setFont(new Font("Helvetica", Font.BOLD, 30));
        textLabel.setHorizontalAlignment(JLabel.CENTER);
        textLabel.setText("MineGame");
        textPanel.setLayout(new BorderLayout());
        textPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        textPanel.add(textLabel, BorderLayout.CENTER);

        resetButton.setBackground(Color.LIGHT_GRAY);
        resetButton.setFocusable(false);
        resetButton.setFont(new Font("Helvetica", Font.BOLD, 30));
        resetButton.addActionListener(e -> resetGame());
        
        newGameButton.setBackground(Color.LIGHT_GRAY);
        newGameButton.setFocusable(false);
        newGameButton.setFont(new Font("Helvetica", Font.BOLD, 30));
        newGameButton.addActionListener(e -> chooseDifficulty());

        textPanel.add(resetButton, BorderLayout.EAST);
        textPanel.add(newGameButton, BorderLayout.WEST);


        //adding the panels to the main panel (can be switched on and off as needed)
        
        frame.add(textPanel, BorderLayout.NORTH);

        gridConstraints.gridy = 0;
        gridConstraints.fill = GridBagConstraints.NONE; // don't stretch the grid
        gridConstraints.weightx = 0.0;
        gridConstraints.weighty = 0.0;
        mainPanel.add(gridPanel, gridConstraints);

        frame.add(choicePanel, BorderLayout.SOUTH);

        difficultyConstraints.gridy = 0;
        difficultyConstraints.fill = GridBagConstraints.HORIZONTAL;
        difficultyConstraints.weightx = 1.0;
        mainPanel.add(difficultyPanel, difficultyConstraints);

        //chooseDifficulty(); //method to choose difficulty (sets up difficultyPanel)
        chooseDifficulty();

        frame.setVisible(true);
    }


    //GAME LOGIC METHODS:

    private boolean inBounds(int r, int c){
        return (r >= 0 && r < rows && c >= 0 && c < cols);
    }


    //BOARD GENERATION METHODS:

    //generateMines(): method to randomly place mines on the board
    private void generateMines(int firstClickRow, int firstClickCol, int safeRadius, int totalMines){
    
        //generate array of all positions
        int[][] indexes = new int[this.rows * this.cols][2];
        for (int r=0; r<this.rows; r++){
            for (int c=0; c<this.cols; c++){
                indexes[r*this.cols+c][0]=r;
                indexes[r*this.cols+c][1]=c;
            }
        }
        //shuffle positions
        for (int i=indexes.length-1; i>0; i--){
            int randIndex = rand.nextInt(i+1);
            int[] temp = indexes[i];
            indexes[i] = indexes[randIndex];
            indexes[randIndex] = temp;
        }

        //place mines, avoiding safe radius around first click
        int minesPlaced = 0;
        for (int[] cell: indexes){
            if (minesPlaced <= totalMines){  //stop if all mines placed
                int r = cell[0];
                int c = cell[1];
                
                if(!(Math.abs(r - firstClickRow) <= safeRadius && Math.abs(c - firstClickCol) <= safeRadius)){
                    //setMine() if not inside safe radius
                    cellBoard[r][c].setMine();
                    //buttons[r][c].setText("M"); //for testing purposes
                    minesPlaced++;
                    iterativeCellNumbers(r, c); //INCREMENTS ALL ADJACENT CELLS' adjacentMine FIELDS (if not bombs)
                                                //and iterates on all "zeros"
                
                }
            }
        }
        this.totalMines = minesPlaced;

    
        //check for pre-flagged mines
        if (incorrectFlags>0){
            for (int k=0;k<totalMines;k++){
                int r = indexes[k][0];
                int c = indexes[k][1];
                if (cellBoard[r][c].isFlagged){
                    incorrectFlags--;
                    correctFlags++;
                }
            }
        }

    }

    //iterativeCellNumbers(): sets all adjacentMines fields of non-bombs
    private void iterativeCellNumbers(int row, int col){
        
        for (int dr=-1;dr<2;dr++){
            for (int dc=-1;dc<2;dc++){
                if(inBounds(row-dr, col-dc)){
                    if (!(dr==0 && dc==0) && !(cellBoard[row-dr][col-dc].isMine)){
                        
                        //increments the adjacentMines field
                        cellBoard[row-dr][col-dc].incrementAdjacentMines();

                    }
                }
            }
        }

    }





    //GAME LOGIC METHODS (and GUI):

    //chooseDifficulty(): method to choose difficulty
    private void chooseDifficulty(){

        //reset difficulty panel
        difficultyPanel.removeAll();
        difficultyPanel.revalidate();
        difficultyPanel.repaint();

        textLabel.setText("MineGame");
        newGameButton.setVisible(false);
        resetButton.setVisible(false);
        difficultyPanel.setLayout(new GridLayout(7,1,50,50));
        difficultyPanel.setBorder(BorderFactory.createEmptyBorder(70,600,70,600));


        //custom difficulty sliders setup
        rowLabel.setFont(new Font("Helvetica",Font.BOLD, 30));
        rowLabel.setHorizontalAlignment(SwingConstants.CENTER);
        difficultyPanel.add(rowLabel);
        rowSlider.setMinimum(10);
        rowSlider.setMaximum(40);
        rowSlider.setValue(15);
        rowLabel.setText("Rows: " + rowSlider.getValue());
        rowSlider.addChangeListener(e -> {
            int v = rowSlider.getValue();
            rowLabel.setText("Rows: " + v);
            sliderUpdate();
        });
        difficultyPanel.add(rowSlider);

        colLabel.setFont(new Font("Helvetica",Font.BOLD, 30));
        colLabel.setHorizontalAlignment(SwingConstants.CENTER);
        difficultyPanel.add(colLabel);
        colSlider.setMinimum(10);
        colSlider.setMaximum(70);
        colSlider.setValue(25);
        colLabel.setText("Columns: " + colSlider.getValue());
        colSlider.addChangeListener(e -> {
            int v = colSlider.getValue();
            colLabel.setText("Columns: " + v);
            sliderUpdate();
        });
        difficultyPanel.add(colSlider);

        mineLabel.setFont(new Font("Helvetica",Font.BOLD, 30));
        mineLabel.setHorizontalAlignment(SwingConstants.CENTER);
        difficultyPanel.add(mineLabel);
        mineSlider.setMinimum(5);
        sliderUpdate();
        mineSlider.setValue(40);
        mineLabel.setText("Mines: " + mineSlider.getValue());
        mineSlider.addChangeListener(e -> {
            int v = mineSlider.getValue();
            mineLabel.setText("Mines: " + v);
        });
        difficultyPanel.add(mineSlider);


        startButton.setFont(new Font("Helvetica",Font.BOLD, 30));
        startButton.setBackground(Color.LIGHT_GRAY);
        startButton.setFocusable(false);
        // set explicit size and keep button centered
        startButton.setPreferredSize(new Dimension(240, 64)); // adjust width/height as desired
        startButton.setMaximumSize(startButton.getPreferredSize());
        startButton.addActionListener(e -> {
            int r = rowSlider.getValue();
            int c = colSlider.getValue();
            int m = mineSlider.getValue();
            startGame(r,c,m);
        });
        JPanel startWrapper = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));
        startWrapper.setOpaque(false);
        startWrapper.add(startButton);
        difficultyPanel.add(startWrapper);



        gridPanel.setVisible(false);
        difficultyPanel.setVisible(true);


        frame.revalidate();
        frame.repaint();
    }

    //sliderUpdate(): method to update slider maximums based on rows and columns
    private void sliderUpdate(){
        int r = rowSlider.getValue();
        int c = colSlider.getValue();
        int m = mineSlider.getValue();
        // Adjust maximum mines based on current rows and columns
        int maxMines = ((r * c)/3);
        mineSlider.setMaximum(maxMines);
        if (m > maxMines) {
            mineSlider.setValue(maxMines);
        }
    }

    //startGame(): method to start the game with selected difficulty
    private void startGame(int rows, int cols,int totalMines){
        
        //reset grid and choice panels
        gridPanel.removeAll();
        gridPanel.revalidate();
        gridPanel.repaint();
        
        choicePanel.removeAll();
        choicePanel.revalidate();
        choicePanel.repaint();
        
        //initialize game state
        this.rows = rows;
        this.cols = cols;
        this.totalMines = totalMines;
        this.correctFlags = 0;
        this.incorrectFlags = 0;
        this.revealedSafeCells = 0;
        this.mode = false; //start in reveal mode
        this.hasWon = false;
        this.firstClick = true;
        textLabel.setText("MineGame");
        newGameButton.setVisible(true);
        resetButton.setVisible(true);
        
        difficultyPanel.setVisible(false);
        gridPanel.setVisible(true);
        //initialize cell board
        cellBoard = new Cell[rows][cols];
        for(int r=0; r<rows; r++){
            for(int c=0; c<cols; c++){
                cellBoard[r][c] = new Cell();
            }
        }

        buttons = new JButton[rows][cols]; //initialize button array
        gridPanel.setLayout(new GridLayout(rows, cols));
        for(int r=0; r<rows; r++){
            for(int c=0; c<cols; c++){
                final int row = r;
                final int col = c;
                
                buttons[r][c] = new JButton();
                buttons[r][c].setMargin(new Insets(1,1,1,1));
                
                buttons[r][c].setFocusPainted(false);
                buttons[r][c].setBackground(Color.WHITE);
                
                buttons[r][c].addMouseListener(new MouseAdapter(){
                    @Override
                    public void mouseClicked(MouseEvent e){

                        if (SwingUtilities.isLeftMouseButton(e)){
                            //left click functionality
                            if (!mode){
                                revealCell(row, col);
                            } else{
                                flagCell(row, col);
                            }
                            
                        } else if (SwingUtilities.isRightMouseButton(e)){
                            //right click functionality
                            if (!mode){
                                flagCell(row, col);
                            } else{
                                revealCell(row, col);
                            }
                            
                        }
                        //checkWinCondition();
                    }
                });
                gridPanel.add(buttons[r][c]);
            }
        }
        
        // compute buttonsize from the real available area (prefer content size, fall back to screen)
        Dimension content = frame.getContentPane().getSize();
        if (content.width <= 0 || content.height <= 0) {
            content = screen; // before visible, fall back to full screen
        }
        
        
        buttonsize = Math.min((content.width - 40) / cols, (content.height - 200) / rows);

        // apply sizes to buttons, icons and grid
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                buttons[r][c].setPreferredSize(new Dimension(buttonsize, buttonsize));
                buttons[r][c].setMaximumSize(new Dimension(buttonsize, buttonsize));
                buttons[r][c].setFont(new Font("Helvetica", Font.BOLD,(int)Math.ceil(buttons[r][c].getPreferredSize().height / 1.5)));
            }
        }
        gridPanel.setPreferredSize(new Dimension(buttonsize * cols, buttonsize * rows));
        gridPanel.setMaximumSize(gridPanel.getPreferredSize());
        flagIcon.setImage(flagIcon.getImage().getScaledInstance(Math.ceilDiv(buttonsize+10,2)*2, Math.ceilDiv(buttonsize+10,2)*2, java.awt.Image.SCALE_SMOOTH));
        mineIcon.setImage(mineIcon.getImage().getScaledInstance(Math.ceilDiv(buttonsize-10,2)*2, Math.ceilDiv(buttonsize-10,2)*2, java.awt.Image.SCALE_SMOOTH));

       
        /* 
        */
        gridConstraints.gridy = 1;
        gridConstraints.fill = GridBagConstraints.NONE;
        gridConstraints.weightx = 0.0;
        gridConstraints.weighty = 0.0;
        mainPanel.add(gridPanel, gridConstraints);

        
    
        /*
        choiceButtons=new JButton[2];

        mainConstraints.gridy = 2;
        mainConstraints.fill = GridBagConstraints.HORIZONTAL;
        mainConstraints.weightx = 1.0;
        mainPanel.add(choicePanel, mainConstraints);
        */
        gridPanel.revalidate();
        gridPanel.repaint();
        

        frame.revalidate();
        frame.repaint();
    }



    //FUNCTIONALITY METHODS:
    //revealCell(): method to reveal a cell
    private void revealCell(int row, int col){
        
        if ((firstClick && !cellBoard[row][col].isFlagged() && !cellBoard[row][col].isRevealed())) {
            generateMines(row, col, safeRadius, totalMines);
            firstClick = false;
        }
        if (!cellBoard[row][col].isFlagged() /*&& !cellBoard[row][col].isRevealed()*/){
            if (cellBoard[row][col].isMine()){
                //hit a mine - game over
                
                //reveal all mines
                endGame(hasWon);
            
            } else{
                //hit a safe cell- reveal it
                iterativeReveal(row, col);

                //if hit a revealed cell and all adjacent mines are flagged, reveal the rest
                if (cellBoard[row][col].isRevealed()){
                    boolean allMinesFlagged=true;
                    for (int dr=-1;dr<=1;dr++){
                        for (int dc=-1;dc<=1;dc++){
                            if (inBounds(row+dr, col+dc) && !(dr==0 && dc==0)){
                                if ((cellBoard[row+dr][col+dc].isMine() && !cellBoard[row+dr][col+dc].isFlagged()) ||
                                    (!cellBoard[row+dr][col+dc].isMine() && cellBoard[row+dr][col+dc].isFlagged())){
                                    allMinesFlagged=false;
                                }
                            }
                        }
                    }
                    if (allMinesFlagged) {
                        for (int dr=-1;dr<=1;dr++){
                            for (int dc=-1;dc<=1;dc++){
                                if (inBounds(row+dr, col+dc) && !(dr==0 && dc==0)){
                                    iterativeReveal(row+dr, col+dc);
                                }
                            }
                        }
                    }
                }

                checkWinCondition();
            }
        }
    
    }

    //flagCell(): method to flag/unflag a cell
    private void flagCell(int row, int col){
        if(!cellBoard[row][col].isRevealed()){
            if (cellBoard[row][col].isMine() && !firstClick) {
                correctFlags = (!cellBoard[row][col].isFlagged()) ? correctFlags+1 : correctFlags-1 ;
            } else {
                incorrectFlags = (!cellBoard[row][col].isFlagged()) ? incorrectFlags+1 : incorrectFlags-1 ;
            }
            
            cellBoard[row][col].toggleFlag();
            buttons[row][col].setIcon((cellBoard[row][col].isFlagged())? flagIcon : null);
        }
        checkWinCondition();
    }

    //iterativeReveal(): method to iteratively reveal cells (flood fill)
    private void iterativeReveal(int row, int col){
        int adjacentMines = cellBoard[row][col].getAdjacentMines();
        if (!cellBoard[row][col].isRevealed() && !cellBoard[row][col].isFlagged()){
            cellBoard[row][col].reveal();
            revealedSafeCells++;

            if (adjacentMines == 0){
                //recursively reveal adjacent cells
                for (int dr=-1; dr<=1; dr++){
                    for (int dc=-1; dc<=1; dc++){
                        if (inBounds(row+dr, col+dc) && !(dr==0 && dc==0) ) {
                            iterativeReveal(row+dr, col+dc);
                        }
                    }
                }
            }
        }
        //color and label the cell at end of recursion
        if (!cellBoard[row][col].isFlagged()){
            if (adjacentMines > 0){
                buttons[row][col].setText(Integer.toString(adjacentMines));
                buttons[row][col].setForeground(colors[adjacentMines - 1]);
            }
            buttons[row][col].setBackground(Color.LIGHT_GRAY);
        }
    }

    //checkWinCondition(): method to check if the player has won
    private void checkWinCondition(){
        if ((correctFlags == totalMines && incorrectFlags == 0) || (revealedSafeCells == (rows * cols - totalMines))){
            this.hasWon = true;
            endGame(true);
        }
    }

    private void endGame(boolean hasWon){
        
        //reveal all mines
        for (int r=0; r<rows; r++){
            for (int c=0; c<cols; c++){
                if (cellBoard[r][c].isMine()){
                    buttons[r][c].setIcon(mineIcon);
                    //highlight all mines
                    buttons[r][c].setBackground((hasWon)?Color.LIGHT_GRAY:Color.RED);
                } else if (cellBoard[r][c].isFlagged() && !cellBoard[r][c].isMine()){
                    //highlight incorrectly flagged cells
                    buttons[r][c].setBackground(Color.YELLOW);
                }
                buttons[r][c].setEnabled(false);
            }
        }
        if (hasWon){
            textLabel.setText("You Win!");
        } else{
            textLabel.setText("Game Over! You hit a mine.");
            
        }

    }

    //resetGame(): method to reset the game
    private void resetGame(){
        startGame(rows, cols, totalMines);
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new MineGame();
            }
        });
    }
}