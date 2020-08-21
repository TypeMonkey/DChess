package jg.proj.chess.net.client.uis.components;

import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

public class GraphicalSquare {

  private final StackPane pane;
  private final Rectangle shape;
  private final Rectangle frame;
  private final Color orgSqColor;
  
  public GraphicalSquare(StackPane pane, Rectangle outline, Rectangle frame, Color originalSquareColor) {
    this.pane = pane;
    this.shape = outline;
    this.frame = frame;
    this.orgSqColor = originalSquareColor;
  }
  
  public Color getOrigialSquareColor() {
    return orgSqColor;
  }
  
  public void setPicture(Image image) {
    frame.setFill(new ImagePattern(image));
  }
  
  public void setPicture(Paint image) {
    frame.setFill(image);
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
