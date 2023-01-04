import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

//Represents a single square of the game area
class Cell {
  // In logical coordinates, with the origin at the top-left corner of the screen
  int x;
  int y;
  Color color;

  // the four adjacent cells to this one
  Cell left;
  Cell top;
  Cell right;
  Cell bottom;

  Cell(int x, int y, Color color) {
    this.x = x;
    this.y = y;
    this.color = color;
  }

  // draws the cell as a worldimage
  WorldImage drawCell() {
    return new RectangleImage(FloodItWorld.CELL_SIZE, FloodItWorld.CELL_SIZE, OutlineMode.SOLID,
        this.color);
  }
}

class FloodItWorld extends World {
  ArrayList<Color> colors;
  int size;
  ArrayList<ArrayList<Cell>> board;
  Random rand = new Random();
  boolean gameOver;
  int totalClicks;
  int clicksSoFar;
  int minutes;
  double seconds;

  // fields exclusively used for onTick()
  ArrayList<Cell> updatingCells;
  Color newColor;
  Color topLeftColor;

  static int CELL_SIZE = 25;

  // constructor to make the board
  FloodItWorld(int size, ArrayList<Color> colors) {
    this.colors = colors;
    this.size = new Utils().checkRange(size);
    this.board = this.makeBoard();
    this.connectCells();
    this.gameOver = false;
    this.totalClicks = (7 * this.size / 4) - (1 / 2);
    this.clicksSoFar = 0;
    // this.minutes = 0;
    // this.seconds = 0;

    // fields only for onTick method
    this.updatingCells = new ArrayList<Cell>();
    this.newColor = this.board.get(0).get(0).color;
    this.topLeftColor = this.newColor;
  }

  // makes the array list of array list of cells as the board
  ArrayList<ArrayList<Cell>> makeBoard() {
    ArrayList<ArrayList<Cell>> board = new ArrayList<ArrayList<Cell>>();

    for (int i = 0; i < this.size; i++) {

      ArrayList<Cell> column = new ArrayList<Cell>();

      for (int j = 0; j < this.size; j++) {
        column.add(new Cell(i, j, colors.get(this.rand.nextInt(this.colors.size()))));

      }
      board.add(column);
    }
    return board;
  }

  // connects the cells to their neighbors
  void connectCells() {
    for (int i = 0; i < this.size; i++) {

      for (int j = 0; j < this.size; j++) {
        Cell c = board.get(i).get(j);

        if (j == board.get(i).size() - 1) {
          c.bottom = c;
        }
        else {
          c.bottom = board.get(i).get(j + 1);
        }

        if (j == 0) {
          c.top = c;
        }
        else {
          c.top = board.get(i).get(j - 1);
        }

        if (i == board.get(j).size() - 1) {
          c.right = c;
        }
        else {
          c.right = board.get(i + 1).get(j);
        }

        if (i == 0) {
          c.left = c;
        }
        else {
          c.left = board.get(i - 1).get(j);
        }
      }
    }
  }

  // makes scene
  public WorldScene makeScene() {
    WorldScene ws = this.getEmptyScene();
    for (ArrayList<Cell> column : this.board) {
      for (Cell c : column) {
        ws.placeImageXY(c.drawCell(), (c.x + 1) * FloodItWorld.CELL_SIZE,
            (c.y + 1) * FloodItWorld.CELL_SIZE);
        ws.placeImageXY(
            new TextImage(
                Integer.toString(this.clicksSoFar) + "/" + (Integer.toString(this.totalClicks)), 24,
                FontStyle.BOLD, Color.black),
            FloodItWorld.CELL_SIZE * this.board.size() / 2,
            FloodItWorld.CELL_SIZE * this.board.size() + (2 * FloodItWorld.CELL_SIZE));
      }
    }
    if (this.gameOver()) {
      ws.placeImageXY(new TextImage(this.gameOverMessage(), 28, FontStyle.BOLD, Color.black),
          FloodItWorld.CELL_SIZE * this.board.size() / 2,
          FloodItWorld.CELL_SIZE * this.board.size() + (4 * FloodItWorld.CELL_SIZE));
    }

    return ws;
  }

  // handles key inputs, "r" will reset the board
  public void onKeyEvent(String s) {
    if (s.equals("r")) {
      this.board = this.makeBoard();
      this.connectCells();
      this.clicksSoFar = 0;
      this.gameOver = false;
    }
  }

  // click to change the flood colors
  public void onMouseClicked(Posn pos) {

    if (this.gameOver) {
      return;
    }
    if (!this.updatingCells.isEmpty()) {
      return;
    }
    int offset = FloodItWorld.CELL_SIZE / 2;
    if (pos.x < offset || pos.x >= offset + (CELL_SIZE * this.size) || pos.y < offset
        || pos.y >= offset + (CELL_SIZE * this.size)) {
      return;
    }

    int x = (pos.x - offset) / FloodItWorld.CELL_SIZE;
    int y = (pos.y - offset) / FloodItWorld.CELL_SIZE;

    Color newColor = this.board.get(x).get(y).color;
    Cell topLeftCell = this.board.get(0).get(0);
    if (!topLeftCell.color.equals(newColor)) {
      this.clicksSoFar = this.clicksSoFar + 1;
      this.updatingCells.add(topLeftCell);
      this.newColor = newColor;
      this.topLeftColor = topLeftCell.color;
    }
  }

  // tick handler
  public void onTick() {

    ArrayList<Cell> updating = new ArrayList<Cell>();

    for (Cell c : this.updatingCells) {

      if (c.color.equals(this.topLeftColor)) {
        c.color = this.newColor;
        updating.add(c.top);
        updating.add(c.bottom);
        updating.add(c.left);
        updating.add(c.right);
      }
      this.updatingCells = updating;
      this.gameOver = this.gameOver();
    }
  }

  // is the game over?
  boolean gameOver() {
    if (this.clicksSoFar != this.totalClicks || !this.updatingCells.isEmpty()) {
      return this.sameColors();
    }
    else {
      return true;
    }
  }

  // returns the message to be displayed when the game is over
  String gameOverMessage() {
    if (this.sameColors()) {
      return "You Win! poggers!";
    }
    else {
      return "You Lose! sadge";
    }
  }

  // are all the cells on the board the same colors?
  boolean sameColors() {
    Color color = this.board.get(0).get(0).color;
    for (ArrayList<Cell> column : this.board) {
      for (Cell c : column) {
        if (!c.color.equals(color)) {
          return false;
        }
      }
    }
    return true;
  }
}

// checks whether the arguments in constructors pass
class Utils {

  // returns the given integer if the size is greater than 0
  // otherwise throws an exception
  public int checkRange(int size) {
    if (size > 0) {
      return size;
    }
    else {
      throw new IllegalArgumentException("Board size must be greater than 0.");
    }
  }
}

//examples class
class ExamplesFlood {

  ArrayList<Color> colors = new ArrayList<Color>(
      Arrays.asList(Color.red, Color.blue, Color.pink, Color.magenta, Color.green, Color.yellow));
  ArrayList<Color> colorsSizeOne = new ArrayList<Color>(Arrays.asList(Color.red));
  Cell redCell = new Cell(0, 0, Color.red);
  Cell blueCell = new Cell(0, 0, Color.blue);
  ArrayList<ArrayList<Cell>> exampleBoard;
  FloodItWorld exampleWorld1;
  FloodItWorld exampleWorld2;

  // initialize the data
  void initData() {
    this.exampleBoard = new ArrayList<ArrayList<Cell>>(Arrays.asList(
        new ArrayList<Cell>(Arrays.asList(new Cell(0, 0, Color.blue), new Cell(0, 1, Color.red))),
        new ArrayList<Cell>(
            Arrays.asList(new Cell(1, 0, Color.green), new Cell(1, 1, Color.yellow)))));
    this.exampleWorld1 = new FloodItWorld(2, this.colors);
    this.exampleWorld1.board = this.exampleBoard;
    this.exampleWorld1.connectCells();
    this.exampleWorld2 = new FloodItWorld(1, this.colorsSizeOne);
  }

  // test for drawCell
  void testDrawCell(Tester t) {
    t.checkExpect(this.redCell.drawCell(), new RectangleImage(FloodItWorld.CELL_SIZE,
        FloodItWorld.CELL_SIZE, OutlineMode.SOLID, redCell.color));
    t.checkExpect(this.blueCell.drawCell(), new RectangleImage(FloodItWorld.CELL_SIZE,
        FloodItWorld.CELL_SIZE, OutlineMode.SOLID, blueCell.color));
  }

  // tests for connectCells
  void testConnectCells(Tester t) {
    initData();
    t.checkExpect(this.exampleWorld1.board.get(0).get(0).right,
        this.exampleWorld1.board.get(1).get(0));
    t.checkExpect(this.exampleWorld1.board.get(1).get(1).right,
        this.exampleWorld1.board.get(1).get(1));
    t.checkExpect(this.exampleWorld1.board.get(0).get(0).left,
        this.exampleWorld1.board.get(0).get(0));
    t.checkExpect(this.exampleWorld1.board.get(1).get(0).left,
        this.exampleWorld1.board.get(0).get(0));
    t.checkExpect(this.exampleWorld1.board.get(1).get(1).top,
        this.exampleWorld1.board.get(1).get(0));
    t.checkExpect(this.exampleWorld1.board.get(0).get(0).top,
        this.exampleWorld1.board.get(0).get(0));
    t.checkExpect(this.exampleWorld1.board.get(0).get(0).bottom,
        this.exampleWorld1.board.get(0).get(1));
    t.checkExpect(this.exampleWorld1.board.get(1).get(1).bottom,
        this.exampleWorld1.board.get(1).get(1));

  }

  // tests for makeBoard
  void testMakeBoard(Tester t) {
    initData();
    t.checkExpect(this.exampleWorld1.makeBoard().size(), 2);
  }

  // tests for checkRange
  void testCheckRange(Tester t) {
    t.checkExpect(new Utils().checkRange(14), 14);
    t.checkExpect(new Utils().checkRange(1), 1);
    t.checkException(new IllegalArgumentException("Board size must be greater than 0."),
        new Utils(), "checkRange", 0);
    t.checkException(new IllegalArgumentException("Board size must be greater than 0."),
        new Utils(), "checkRange", -2);
  }

  // test for makeScene
  void testMakeScene(Tester t) {
    initData();
    WorldScene ws = this.exampleWorld2.getEmptyScene();
    ws.placeImageXY(this.redCell.drawCell(), FloodItWorld.CELL_SIZE, FloodItWorld.CELL_SIZE);
    ws.placeImageXY(
        new TextImage(Integer.toString(0) + "/" + Integer.toString(1), 24, FontStyle.BOLD,
            Color.black),
        FloodItWorld.CELL_SIZE * (1 / 2),
        FloodItWorld.CELL_SIZE * 1 + (2 * FloodItWorld.CELL_SIZE));
    t.checkExpect(this.exampleWorld2.makeScene(), ws);
  }

  // test for onKeyEvent
  void testOnKeyEvent(Tester t) {
    initData();
    this.exampleWorld1.gameOver = true;
    t.checkExpect(this.exampleWorld1.gameOver, true);
    this.exampleWorld1.clicksSoFar = 5;
    t.checkExpect(this.exampleWorld1.clicksSoFar, 5);
    this.exampleWorld1.onKeyEvent("n");
    t.checkExpect(this.exampleWorld1.clicksSoFar, 5);
    this.exampleWorld1.onKeyEvent("r");
    t.checkExpect(this.exampleWorld1.gameOver, false);
    t.checkExpect(this.exampleWorld1.clicksSoFar, 0);
  }

  // test for onMouseClicked
  void testOnMouseClicked(Tester t) {
    initData();
    Cell topLeft = this.exampleWorld1.board.get(0).get(0);
    Cell topRight = this.exampleWorld1.board.get(1).get(0);
    t.checkExpect(topLeft.color.equals(topRight.color), false);
    // click the top right cell in a 2x2 world
    this.exampleWorld1.onMouseClicked(new Posn(50, 25));
    this.exampleWorld1.onTick();
    t.checkExpect(topRight.color, topLeft.color);
    initData();
    topLeft = this.exampleWorld1.board.get(0).get(0);
    topRight = this.exampleWorld1.board.get(1).get(0);
    t.checkExpect(topLeft.color.equals(topRight.color), false);
    // click on an area that will not mutate the board / flood the cells with
    // a new color
    this.exampleWorld1.onMouseClicked(new Posn(0, 0));
    this.exampleWorld1.onTick();
    t.checkExpect(topLeft.color.equals(topRight.color), false);
  }

  // test for onTick
  void testOnTick(Tester t) {
    initData();
    Cell topLeft = this.exampleWorld1.board.get(0).get(0);
    Cell topRight = this.exampleWorld1.board.get(1).get(0);
    this.exampleWorld1.onTick();
    t.checkExpect(topLeft.color.equals(topRight.color), false);
    this.exampleWorld1.onMouseClicked(new Posn(50, 25));
    t.checkExpect(topLeft.color.equals(topRight.color), false);
    this.exampleWorld1.onTick();
    t.checkExpect(topRight.color, topLeft.color);
  }

  // test for gameOver
  void testGameOver(Tester t) {
    initData();
    t.checkExpect(this.exampleWorld1.gameOver(), false);
    this.exampleWorld1.clicksSoFar = this.exampleWorld1.totalClicks;
    t.checkExpect(this.exampleWorld1.gameOver(), true);
  }

  // test for gameOverMessage
  void testGameOverMessage(Tester t) {
    initData();
    t.checkExpect(this.exampleWorld1.gameOverMessage(), "You Lose! sadge");
    t.checkExpect(this.exampleWorld2.gameOverMessage(), "You Win! poggers!");
  }

  // test for sameColors
  void testSameColors(Tester t) {
    initData();
    t.checkExpect(this.exampleWorld1.sameColors(), false);
    t.checkExpect(this.exampleWorld2.sameColors(), true);
  }

  // test for BigBang
  void testBigBang(Tester t) {
    double tickRate = .016;
    int width = 500;
    int height = 500;

    FloodItWorld owo = new FloodItWorld(14, this.colors);
    owo.bigBang(width, height, tickRate);
  }
}
