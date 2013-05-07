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
import com.tallordergames.chasetheace.R;

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
			if (!connected)
			{
				serverIpAddress = serverIp.getText().toString();
				yourName = playerName.getText().toString();
				if ((!serverIpAddress.equals("")) && (!yourName.equals("")))
				{
					InputMethodManager localInputMethodManager = (InputMethodManager)getSystemService("input_method");
					localInputMethodManager.hideSoftInputFromWindow(serverIp.getWindowToken(), 0);
					localInputMethodManager.hideSoftInputFromWindow(playerName.getWindowToken(), 0);
					Thread fst = new Thread(new ClientThread());
					fst.start();
					showMsgOverlay();
					msgText.setText("Waiting to start game");
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
			if (connected)
			{
				sendCmdToServer("NEWDEAL");
				dealButton.setVisibility(View.GONE);
				msgText.setText("Waiting on your turn.");
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
	private OnClickListener startGameListener = new OnClickListener()
	{
		public void onClick(View paramView)
		{
			if (connected)
			{
				sendCmdToServer("STARTNEWGAME");
				startGameButton.setVisibility(View.INVISIBLE);
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
				connectionInfoLayout.setVisibility(View.GONE);
				gameInfoLayout.setVisibility(View.VISIBLE);
				showMsgOverlay();
				Toast.makeText(getApplication(), "Connected to server!", Toast.LENGTH_SHORT).show();
			}
		});
	}

	public void hideMsgOverlay()
	{
		this.overlayLayout.setVisibility(View.INVISIBLE);
		this.cardImage.setOnClickListener(this);
		this.cardImage.setOnTouchListener(this.gestureListener);
	}

	public void onClick(View paramView)
	{
	}

	protected void onCreate(Bundle paramBundle)
	{
		super.onCreate(paramBundle);
		setContentView(R.layout.client);
		this.serverIp = ((EditText)findViewById(R.id.server_ip));
		this.playerName = ((EditText)findViewById(R.id.player_name));
		this.connectPhones = ((Button)findViewById(R.id.connect_phones));
		this.dealButton = ((Button)findViewById(R.id.dealButton));
		this.startGameButton = ((Button)findViewById(R.id.newGameButton));
		this.connectionInfoLayout = ((LinearLayout)findViewById(R.id.connectionInfoLayout));
		this.gameInfoLayout = ((LinearLayout)findViewById(R.id.gameInfoLayout));
		this.overlayLayout = ((LinearLayout)findViewById(R.id.overlayLayout));
		this.msgText = ((TextView)findViewById(R.id.msgText));
		this.cardImage = ((ImageView)findViewById(R.id.cardImage));
		this.connectPhones.setOnClickListener(this.connectListener);
		this.dealButton.setOnClickListener(this.dealListener);
		this.startGameButton.setOnClickListener(this.startGameListener);
		this.startGameButton.setVisibility(View.VISIBLE);
		this.gestureDetector = new GestureDetector(getApplicationContext(), new MyGestureDetector());
		this.gestureListener = new OnTouchListener()
		{
			public boolean onTouch(View paramView, MotionEvent paramMotionEvent)
			{
				return gestureDetector.onTouchEvent(paramMotionEvent);
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
		this.overlayLayout.setVisibility(View.VISIBLE);
		this.overlayLayout.bringToFront();
		this.cardImage.setOnClickListener(null);
		this.cardImage.setOnTouchListener(null);
	}

	public class ClientThread implements Runnable
	{
		public void run()
		{
			Looper.prepare();
			try
			{
				InetAddress localInetAddress = InetAddress.getByName(serverIpAddress);
				Log.d("ClientActivity", "C: Connecting..");
				socket = new Socket(localInetAddress, 8080);
				connected = true;
				sendCmdToServer("NEWPLAYER:" + yourName);
				hideConnectionInfo();
				while (true)
				{
					if (!connected)
					{
						Log.d("ClientActivity", "C: Closed.");
						return;
					}
					try
					{
						BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
						String line = null;
						while ((line = in.readLine()) != null)
						{
							Log.d("ClientActivity", line);
							final Command cmd = new Command(line);
							handler.post(new Runnable() {
								@Override
								public void run() {
									cmd.tick();
								}
							});
						}
					}
					catch (Exception localException2)
					{
						handler.post(new Runnable() {
							@Override
							public void run()
							{
								Intent localIntent = new Intent("CTA_LOG");
								localIntent.putExtra("msg", "Oops. Connection interrupted. Please reconnect your phones.");
								sendBroadcast(localIntent);
							}
						});
						localException2.printStackTrace();
					}
				}
			}
			catch (Exception e)
			{
				Log.e("ClientActivity", "C: Error", e);
				Toast.makeText(getApplication(), "Error connecting to server", Toast.LENGTH_SHORT).show();
				connected = false;
			}
		}
	}

	public class Command
	{
		String cmd;
		String txt;

		public Command(String cmd)
		{
			String[] arrayOfString = cmd.split(":");
			this.cmd = arrayOfString[0];
			if (arrayOfString.length > 1)
				this.txt = arrayOfString[1];
		}

		@SuppressLint({"NewApi"})
		public void tick()
		{
			Log.d("CLIENT", "Ticking : " + this.cmd);
			int i = -1;

			if (this.cmd.equals("SETCARD"))
			{
				playerCard = Integer.parseInt(this.txt);
				i = -1;

				if ((!playerIsDealer) || ((playerIsDealer) && (seenCard)))
				{
					i = playerCard;

					Bitmap localBitmap2 = null;
					try {
						localBitmap2 = BitmapFactory.decodeStream(getAssets().open("cards/" + i + ".png"));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					cardImage.setImageBitmap(localBitmap2);
					ObjectAnimator localObjectAnimator2 = ObjectAnimator.ofFloat(cardImage, "translationX", new float[] { -600.0F, 0.0F });
					localObjectAnimator2.setDuration(400L);
					localObjectAnimator2.start();
				}
			}

			if (this.cmd.equalsIgnoreCase("YOURTURN"))
			{
				hideMsgOverlay();
				if (!playerIsDealer)
				{
					Bitmap localBitmap1 = null;
					try {
						localBitmap1 = BitmapFactory.decodeStream(getAssets().open("cards/" + playerCard + ".png"));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					cardImage.setImageBitmap(localBitmap1);
					seenCard = true;
					ObjectAnimator localObjectAnimator1 = ObjectAnimator.ofFloat(cardImage, "rotationY", new float[] { 360.0F });
					localObjectAnimator1.setDuration(400L);
					localObjectAnimator1.start();
				}
			}

			if (this.cmd.equalsIgnoreCase("SETDEALER"))
			{
				msgText.setText("You are the dealer.");
				dealButton.setVisibility(View.VISIBLE);
				seenCard = false;
				playerIsDealer = true;
			}

			if (this.cmd.equalsIgnoreCase("UNSETDEALER"))
				playerIsDealer = false;

			if (this.cmd.equals("ENDGAME"))
			{
				showMsgOverlay();
				msgText.setText("Game Over!");
			}

			if (this.cmd.equals("WINNER"))
				msgText.setText(msgText.getText() + "\nWinner: " + this.txt);

			if (this.cmd.equals("LOSER"))
				msgText.setText(msgText.getText() + "\nLoser: " + this.txt);

			if (this.cmd.equals("STARTNEWGAME"))
			{
				msgText.setText("Waiting for your turn.");
				startGameButton.setVisibility(View.GONE);
				dealButton.setVisibility(View.GONE);
			}
			return;
		}
	}

	class MyGestureDetector extends GestureDetector.SimpleOnGestureListener
	{
		MyGestureDetector()
		{
		}

		public boolean onDoubleTap(MotionEvent paramMotionEvent)
		{
			Toast.makeText(ClientActivity.this, "Sticking", Toast.LENGTH_SHORT).show();
			sendCmdToServer("STICK");
			if (!playerIsDealer)
			{
				showMsgOverlay();
				msgText.setText("Waiting on others to finish.");
			}
			return true;
		}

		public boolean onFling(MotionEvent paramMotionEvent1, MotionEvent paramMotionEvent2, float paramFloat1, float paramFloat2)
		{
			try
			{
				if (Math.abs(paramMotionEvent1.getY() - paramMotionEvent2.getY()) > SWIPE_MAX_OFF_PATH)
					return false;
				if ((paramMotionEvent1.getX() - paramMotionEvent2.getX() > SWIPE_MIN_DISTANCE) && (Math.abs(paramFloat1) > SWIPE_THRESHOLD_VELOCITY))
				{
					if (connected)
					{
						ObjectAnimator localObjectAnimator = ObjectAnimator.ofFloat(cardImage, "translationX", new float[] { 0.0F, -600.0F });
						localObjectAnimator.setDuration(200L);
						localObjectAnimator.start();
						localObjectAnimator.addListener(new Animator.AnimatorListener()
						{
							public void onAnimationCancel(Animator paramAnimator)
							{
							}

							public void onAnimationEnd(Animator paramAnimator)
							{
								sendCmdToServer("SWAPCARD");
								if (!playerIsDealer)
								{
									showMsgOverlay();
									msgText.setText("Waiting on others to finish.");
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
				else if ((paramMotionEvent2.getX() - paramMotionEvent1.getX() > SWIPE_MIN_DISTANCE) && (Math.abs(paramFloat1) > SWIPE_THRESHOLD_VELOCITY))
					Toast.makeText(ClientActivity.this, "Left Swipe", Toast.LENGTH_SHORT).show();
				return false;
			}
			catch (Exception localException)
			{
			}
			return false;
		}
	}
}