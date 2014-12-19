import java.awt.Dimension;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;
import processing.video.Capture;

public class WhackAMole extends PApplet {
   int speed = 1000; // how many ms the mole stays in the same place
   float redRatio = 3;//how much more red than green and blue is required for detection

   Capture video;
   PFont font;
   PImage mole;// the mole: hit me
   PImage squirrel;// the squirrel: don't hit me
   Clip clip;// soundclip played when hit
   
   int counter = 0;
   int moleX = 0;
   int moleY = 0;
   int score = 0;

   int moleWidth;
   int moleHeight;
   boolean fakeMole = true;
   boolean scored = false;
   boolean debug = false;
   
   @Override
   public void setup() {
      String[] cameras = Capture.list();
      Dimension screenSize = getToolkit().getScreenSize();
      int maxX = 0, maxY = 0;
      String selectedCam = null;
      
      if (cameras.length == 0) {
         println("No cameras found!");
         exit();
      } else {
         println("Available cameras:");
         Pattern p = Pattern.compile(".*size=(\\d+)x(\\d+),fps=(\\d+)");
         for (int i = 0; i < cameras.length; i++) {
            println(cameras[i]);
            Matcher m = p.matcher(cameras[i]);
            if (m.matches()) {
               int fps = Integer.valueOf(m.group(3));
               if (fps >= 30) {
                  int x = Integer.valueOf(m.group(1));
                  int y = Integer.valueOf(m.group(2));
                  if (x > maxX && x <= screenSize.width && y <= screenSize.height) {
                     maxX = x;
                     maxY = y;
                     selectedCam = cameras[i];
                  }
               }
            }
         }
      }
      if (selectedCam != null) {
         video = new Capture(this, selectedCam);//, "name=Logitech HD Webcam C525,size=1280x960,fps=30");
      } else {
         video = new Capture(this);
      }
      println("Selected Cam: " + selectedCam);
      video.start();
      size(maxX, maxY);
      try {
         clip = AudioSystem.getClip();
         clip.open(AudioSystem.getAudioInputStream(new File("aua.wav")));
      } catch (Exception e) {
         e.printStackTrace();
      }
      
      noSmooth();
      fill(color(0, 0, 0, 255));
      stroke(color(0, 255, 0, 255));
      strokeWeight(8);

      font = createFont("Arial", 20, true);
      mole = loadImage("mole-medium.png");
      squirrel = loadImage("squirrel-medium.png");
   }

   @Override
   public void draw() {
      if (video.available()) {
         video.read();
      }
      video.loadPixels();
      // image(video, 0, 0);
      set(0, 0, video);
      int ms = millis();
      if (ms > speed + counter) {
         moleX = (int) (random(video.width - 30)) + 15;
         moleY = (int) (random(video.height - 30)) + 15;
         counter = ms;
         scored = false;
         fakeMole = random(10) > 8;
         if (fakeMole) {
            moleWidth = squirrel.width / 2;
            moleHeight = squirrel.height / 2;
         } else {
            moleWidth = mole.width / 2;
            moleHeight = mole.height / 2;
         }
      }

      for (int x = 0; x < video.width; x++) {
         for (int y = 0; y < video.height; y++) {
            int loc = x + y * video.width;
            int currentColor = video.pixels[loc];

            if (isTracker(currentColor)) {
               if(debug)
                  point(x, y);
               if (!scored && moleX > (x - moleWidth) && moleX < (x + moleWidth)
                     && moleY > (y - moleHeight) && moleY < (y + moleHeight)) {
                  if (fakeMole)
                     score -= 5;
                  else
                     score++;
                  scored = true;
                  // println("Score: " + score);
                  if (fakeMole)
                     stroke(color(255, 0, 0, 255));
                  else
                     stroke(color(0, 255, 0, 255));
                  
                  clip.stop();
                  clip.setMicrosecondPosition(0);
                  clip.start();

                  line(0, 0, width, 0);
                  line(0, 0, 0, height);
                  line(width - 1, height - 1, 0, height - 1);
                  line(width - 1, height - 1, width - 1, 0);
               }
            }
         }
      }
      image(fakeMole ? squirrel : mole, moleX - moleWidth, moleY - moleHeight);

      text("Score:" + score + "\nFPS:" + frameRate, 10, 15);
   }

   boolean isTracker(int c) {
      float r1 = red(c);
      float g1 = green(c);
      float b1 = blue(c);

      // return r1 > red && g1 < green && b1 < blue;
      return g1 * redRatio < r1 && b1 * redRatio < r1;
   }

   @Override
   public void keyPressed() {
      if (key == 'S') {
         speed += 200;
      } else if (key == 's') {
         speed -= 200;
      } else if (key == 'R') {
         redRatio += 0.2;
      } else if (key == 'r') {
         redRatio -= 0.2;
      } else if (key == 'd')
         debug = !debug;
      
      int locMouse = mouseX + mouseY * video.width;
      int color = video.pixels[locMouse];
      
      printRGB(color);
   }

   @Override
   public void mousePressed() {
      int locMouse = mouseX + mouseY * video.width;
      int trackColor = video.pixels[locMouse];

      printRGB(trackColor);
   }

   void printRGB(int color) {
      float red = red(color);
      float green = green(color);
      float blue = blue(color);
      
      println("Red:" + red);
      println("Green:" + green);
      println("Blue:" + blue);
      println("Speed:" + speed);
      println("RedRatio:" + redRatio);
      println();
   }
   
   public static void main(String args[]) {
      PApplet.main(new String[] { "--present", "WhackAMole" });
   }
}