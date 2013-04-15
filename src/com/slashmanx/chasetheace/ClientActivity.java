package com.slashmanx.chasetheace;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.Animator.AnimatorListener;
import com.nineoldandroids.animation.ObjectAnimator;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class ClientActivity extends Activity
  implements View.OnClickListener
{
  private static final int SWIPE_MAX_OFF_PATH = 250;
  private static final int SWIPE_MIN_DISTANCE = 120;
  private static final int SWIPE_THRESHOLD_VELOCITY = 200;
  private ImageView cardImage;
  private View.OnClickListener connectListener = new View.OnClickListener()
  {
    public void onClick(View paramView)
    {
      if (!ClientActivity.this.connected)
      {
        ClientActivity.this.serverIpAddress = ClientActivity.this.serverIp.getText().toString();
        ClientActivity.this.yourName = ClientActivity.this.playerName.getText().toString();
        if ((!ClientActivity.this.serverIpAddress.equals("")) && (!ClientActivity.this.yourName.equals("")))
        {
          InputMethodManager localInputMethodManager = (InputMethodManager)ClientActivity.this.getSystemService("input_method");
          localInputMethodManager.hideSoftInputFromWindow(ClientActivity.this.serverIp.getWindowToken(), 0);
          localInputMethodManager.hideSoftInputFromWindow(ClientActivity.this.playerName.getWindowToken(), 0);
          new Thread(new ClientActivity.ClientThread(ClientActivity.this)).start();
          ClientActivity.this.showMsgOverlay();
          ClientActivity.this.msgText.setText("Waiting to start game");
        }
      }
    }
  };
  private Button connectPhones;
  private boolean connected = false;
  private LinearLayout connectionInfoLayout;
  private Button dealButton;
  private View.OnClickListener dealListener = new View.OnClickListener()
  {
    public void onClick(View paramView)
    {
      if (ClientActivity.this.connected)
      {
        ClientActivity.this.sendCmdToServer("NEWDEAL");
        ClientActivity.this.dealButton.setVisibility(8);
        ClientActivity.this.msgText.setText("Waiting on your turn.");
      }
    }
  };
  private LinearLayout gameInfoLayout;
  private GestureDetector gestureDetector;
  View.OnTouchListener gestureListener;
  private Handler handler = new Handler();
  private TextView msgText;
  private LinearLayout overlayLayout;
  private int playerCard = -1;
  private boolean playerIsDealer = false;
  private EditText playerName;
  private boolean seenCard = true;
  private EditText serverIp;
  private String serverIpAddress = "";
  private Socket socket;
  private Button startGameButton;
  private View.OnClickListener startGameListener = new View.OnClickListener()
  {
    public void onClick(View paramView)
    {
      if (ClientActivity.this.connected)
      {
        ClientActivity.this.sendCmdToServer("STARTNEWGAME");
        ClientActivity.this.startGameButton.setVisibility(4);
      }
    }
  };
  private String yourName = "";

  public void hideConnectionInfo()
  {
    runOnUiThread(new Runnable()
    {
      public void run()
      {
        ClientActivity.this.connectionInfoLayout.setVisibility(8);
        ClientActivity.this.gameInfoLayout.setVisibility(0);
        ClientActivity.this.showMsgOverlay();
        Toast.makeText(ClientActivity.this.getApplication(), "Connected to server!", 1).show();
      }
    });
  }

  public void hideMsgOverlay()
  {
    this.overlayLayout.setVisibility(4);
    this.cardImage.setOnClickListener(this);
    this.cardImage.setOnTouchListener(this.gestureListener);
  }

  public void onClick(View paramView)
  {
  }

  protected void onCreate(Bundle paramBundle)
  {
    super.onCreate(paramBundle);
    setContentView(2130903040);
    this.serverIp = ((EditText)findViewById(2131361795));
    this.playerName = ((EditText)findViewById(2131361794));
    this.connectPhones = ((Button)findViewById(2131361796));
    this.dealButton = ((Button)findViewById(2131361802));
    this.startGameButton = ((Button)findViewById(2131361803));
    this.connectionInfoLayout = ((LinearLayout)findViewById(2131361793));
    this.gameInfoLayout = ((LinearLayout)findViewById(2131361797));
    this.overlayLayout = ((LinearLayout)findViewById(2131361800));
    this.msgText = ((TextView)findViewById(2131361801));
    this.cardImage = ((ImageView)findViewById(2131361799));
    this.connectPhones.setOnClickListener(this.connectListener);
    this.dealButton.setOnClickListener(this.dealListener);
    this.startGameButton.setOnClickListener(this.startGameListener);
    this.startGameButton.setVisibility(0);
    this.gestureDetector = new GestureDetector(getApplicationContext(), new MyGestureDetector());
    this.gestureListener = new View.OnTouchListener()
    {
      public boolean onTouch(View paramView, MotionEvent paramMotionEvent)
      {
        return ClientActivity.this.gestureDetector.onTouchEvent(paramMotionEvent);
      }
    };
    this.cardImage.setOnClickListener(this);
    this.cardImage.setOnTouchListener(this.gestureListener);
  }

  protected void onDestroy()
  {
    super.onDestroy();
    try
    {
      if (this.socket != null)
      {
        sendCmdToServer("DISCONNECT");
        this.connected = false;
        this.socket.close();
      }
      return;
    }
    catch (IOException localIOException)
    {
      localIOException.printStackTrace();
    }
  }

  public void sendCmdToServer(String paramString)
  {
    try
    {
      new PrintWriter(new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream())), true).println(paramString);
      return;
    }
    catch (IOException localIOException)
    {
      localIOException.printStackTrace();
    }
  }

  public void showMsgOverlay()
  {
    this.overlayLayout.setVisibility(0);
    this.overlayLayout.bringToFront();
    this.cardImage.setOnClickListener(null);
    this.cardImage.setOnTouchListener(null);
  }

  public class ClientThread
    implements Runnable
  {
    public ClientThread()
    {
    }

    public void run()
    {
      Looper.prepare();
      try
      {
        InetAddress localInetAddress = InetAddress.getByName(ClientActivity.this.serverIpAddress);
        Log.d("ClientActivity", "C: Connecting..");
        ClientActivity.this.socket = new Socket(localInetAddress, 8080);
        ClientActivity.this.connected = true;
        ClientActivity.this.sendCmdToServer("NEWPLAYER:" + ClientActivity.this.yourName);
        ClientActivity.this.hideConnectionInfo();
        while (true)
        {
          if (!ClientActivity.this.connected);
          while (true)
          {
            Log.d("ClientActivity", "C: Closed.");
            return;
            try
            {
              BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(ClientActivity.this.socket.getInputStream()));
              while (true)
              {
                String str = localBufferedReader.readLine();
                if (str == null)
                  break;
                Log.d("ClientActivity", str);
                ClientActivity.Command localCommand = new ClientActivity.Command(ClientActivity.this, str);
                ClientActivity.this.handler.post(new Runnable(localCommand)
                {
                  public void run()
                  {
                    this.val$cmd.tick();
                  }
                });
              }
            }
            catch (Exception localException2)
            {
              ClientActivity.this.handler.post(new Runnable()
              {
                public void run()
                {
                  Intent localIntent = new Intent("CTA_LOG");
                  localIntent.putExtra("msg", "Oops. Connection interrupted. Please reconnect your phones.");
                  ClientActivity.this.sendBroadcast(localIntent);
                }
              });
              localException2.printStackTrace();
            }
          }
        }
      }
      catch (Exception localException1)
      {
        Log.e("ClientActivity", "C: Error", localException1);
        Toast.makeText(ClientActivity.this.getApplication(), "Error connecting to server", 1).show();
        ClientActivity.this.connected = false;
      }
    }
  }

  public class Command
  {
    String cmd;
    String txt;

    public Command(String arg2)
    {
      Object localObject;
      String[] arrayOfString = localObject.split(":");
      this.cmd = arrayOfString[0];
      if (arrayOfString.length > 1)
        this.txt = arrayOfString[1];
    }

    @SuppressLint({"NewApi"})
    public void tick()
    {
      Log.d("CLIENT", "Ticking : " + this.cmd);
      int i;
      if (this.cmd.equals("SETCARD"))
      {
        ClientActivity.this.playerCard = Integer.parseInt(this.txt);
        i = -1;
      }
      try
      {
        if ((!ClientActivity.this.playerIsDealer) || ((ClientActivity.this.playerIsDealer) && (ClientActivity.this.seenCard)))
          i = ClientActivity.this.playerCard;
        Bitmap localBitmap2 = BitmapFactory.decodeStream(ClientActivity.this.getAssets().open("cards/" + i + ".png"));
        ClientActivity.this.cardImage.setImageBitmap(localBitmap2);
        ObjectAnimator localObjectAnimator2 = ObjectAnimator.ofFloat(ClientActivity.this.cardImage, "translationX", new float[] { -600.0F, 0.0F });
        localObjectAnimator2.setDuration(400L);
        localObjectAnimator2.start();
        if (this.cmd.equalsIgnoreCase("YOURTURN"))
        {
          ClientActivity.this.hideMsgOverlay();
          if (!ClientActivity.this.playerIsDealer);
        }
      }
      catch (IOException localIOException2)
      {
        try
        {
          Bitmap localBitmap1 = BitmapFactory.decodeStream(ClientActivity.this.getAssets().open("cards/" + ClientActivity.this.playerCard + ".png"));
          ClientActivity.this.cardImage.setImageBitmap(localBitmap1);
          ClientActivity.this.seenCard = true;
          ObjectAnimator localObjectAnimator1 = ObjectAnimator.ofFloat(ClientActivity.this.cardImage, "rotationY", new float[] { 360.0F });
          localObjectAnimator1.setDuration(400L);
          localObjectAnimator1.start();
          if (this.cmd.equalsIgnoreCase("SETDEALER"))
          {
            ClientActivity.this.msgText.setText("You are the dealer.");
            ClientActivity.this.dealButton.setVisibility(0);
            ClientActivity.this.seenCard = false;
            ClientActivity.this.playerIsDealer = true;
          }
          if (this.cmd.equalsIgnoreCase("UNSETDEALER"))
            ClientActivity.this.playerIsDealer = false;
          if (this.cmd.equals("ENDGAME"))
          {
            ClientActivity.this.showMsgOverlay();
            ClientActivity.this.msgText.setText("Game Over!");
          }
          if (this.cmd.equals("WINNER"))
            ClientActivity.this.msgText.setText(ClientActivity.this.msgText.getText() + "\nWinner: " + this.txt);
          if (this.cmd.equals("LOSER"))
            ClientActivity.this.msgText.setText(ClientActivity.this.msgText.getText() + "\nLoser: " + this.txt);
          if (this.cmd.equals("STARTNEWGAME"))
          {
            ClientActivity.this.msgText.setText("Waiting for your turn.");
            ClientActivity.this.startGameButton.setVisibility(8);
            ClientActivity.this.dealButton.setVisibility(8);
          }
          return;
          localIOException2 = localIOException2;
          localIOException2.printStackTrace();
        }
        catch (IOException localIOException1)
        {
          while (true)
            localIOException1.printStackTrace();
        }
      }
    }
  }

  class MyGestureDetector extends GestureDetector.SimpleOnGestureListener
  {
    MyGestureDetector()
    {
    }

    public boolean onDoubleTap(MotionEvent paramMotionEvent)
    {
      Toast.makeText(ClientActivity.this, "Sticking", 0).show();
      ClientActivity.this.sendCmdToServer("STICK");
      if (!ClientActivity.this.playerIsDealer)
      {
        ClientActivity.this.showMsgOverlay();
        ClientActivity.this.msgText.setText("Waiting on others to finish.");
      }
      return true;
    }

    public boolean onFling(MotionEvent paramMotionEvent1, MotionEvent paramMotionEvent2, float paramFloat1, float paramFloat2)
    {
      try
      {
        if (Math.abs(paramMotionEvent1.getY() - paramMotionEvent2.getY()) > 250.0F)
          return false;
        if ((paramMotionEvent1.getX() - paramMotionEvent2.getX() > 120.0F) && (Math.abs(paramFloat1) > 200.0F))
        {
          if (ClientActivity.this.connected)
          {
            ObjectAnimator localObjectAnimator = ObjectAnimator.ofFloat(ClientActivity.this.cardImage, "translationX", new float[] { 0.0F, -600.0F });
            localObjectAnimator.setDuration(200L);
            localObjectAnimator.start();
            localObjectAnimator.addListener(new Animator.AnimatorListener()
            {
              public void onAnimationCancel(Animator paramAnimator)
              {
              }

              public void onAnimationEnd(Animator paramAnimator)
              {
                ClientActivity.this.sendCmdToServer("SWAPCARD");
                if (!ClientActivity.this.playerIsDealer)
                {
                  ClientActivity.this.showMsgOverlay();
                  ClientActivity.this.msgText.setText("Waiting on others to finish.");
                }
              }

              public void onAnimationRepeat(Animator paramAnimator)
              {
              }

              public void onAnimationStart(Animator paramAnimator)
              {
              }
            });
            return false;
          }
        }
        else if ((paramMotionEvent2.getX() - paramMotionEvent1.getX() > 120.0F) && (Math.abs(paramFloat1) > 200.0F))
          Toast.makeText(ClientActivity.this, "Left Swipe", 0).show();
        return false;
      }
      catch (Exception localException)
      {
      }
      return false;
    }
  }
}

/* Location:           /Users/emartin/Downloads/CTA/classes-dex2jar.jar
 * Qualified Name:     com.slashmanx.chasetheace.ClientActivity
 * JD-Core Version:    0.6.0
 */