package jg.proj.chess.net.client.uis.components;

import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

public class GraphicalSquare {

  private final StackPane pane;
  private final Rectangle shape;
  private final Rectangle frame;
  
  public GraphicalSquare(StackPane pane, Rectangle outline, Rectangle frame) {
    this.pane = pane;
    this.shape = outline;
    this.frame = frame;
  }
  
  public void setPicture(Image image) {
    frame.setFill(new ImagePattern(image));
  }
  
  public Paint getPicture() {
    return frame.getFill();
  }
  
  public Rectangle getFrame() {
    return frame;
  }
  
  public StackPane getPane() {
    return pane;
  }
  
  public Rectangle getOutline() {
    return shape;
  }
}
