package jg.proj.chess.net.client.uis.components;

import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

public class GraphicalSquare {

  private final StackPane pane;
  private final Rectangle shape;
  
  public GraphicalSquare(StackPane pane, Rectangle shape) {
    this.pane = pane;
    this.shape = shape;
  }
  
  public StackPane getPane() {
    return pane;
  }
  
  public Rectangle getShape() {
    return shape;
  }
}
