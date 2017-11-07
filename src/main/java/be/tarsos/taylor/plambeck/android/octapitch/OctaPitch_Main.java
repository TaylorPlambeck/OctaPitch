package be.tarsos.taylor.plambeck.android.octapitch;

/*
           ***  Created By:  ***
		      Taylor Plambeck
		   OctaPitch - Guitar Tuner
	   Built from September 2016 - June 2017
    California Polytechnic University of Pomona
  College of Computer Engineering Senior Project
	(Pitch Recognition Credit to TarsosDSP!)

 */

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;
// Imports added by T.P.
import android.app.ActionBar;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.EmbossMaskFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.DecimalFormat;
//have my own dispatcher
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import android.widget.Toast;

public class OctaPitch_Main extends ActionBarActivity {

	// establish Paint, Bitmap and Canvas to draw the shapes
	Paint paint    = new Paint();
	Paint paintLT1 = new Paint();
	Paint paintLT2 = new Paint();
	Paint paintLT3 = new Paint();
	Paint paintLT4 = new Paint();
	Paint paintRT1 = new Paint();
	Paint paintRT2 = new Paint();
	Paint paintRT3 = new Paint();
	Paint paintRT4 = new Paint();
	Paint paintCircle = new Paint();
	Paint paintMiniCircle = new Paint();
	Bitmap bg= Bitmap.createBitmap(600,1000,Bitmap.Config.ARGB_8888);	//600,1000 used to draw shapes
	Canvas canvas = new Canvas(bg);

	// COLOR PALETTE
	//FOR HEX, we have 6 slots, each representing 4 bits.   0000-0000-0000-0000-0000-0000
	//		the 8 bits represent value 0-255				(  RED  ) (  GRN  ) (  BLUE  )
	String PINK="#AA3E62";
	String BLUE_DARK="#04486F";
	String BLUE_LIGHT="#74C7CD";
	String YELLOW_PALE="#F1EB74";
	String BG_COLOR="#AF9BA6";
	String ORANGE="#FF7F27";
	// Flags and duplicates to enforce 90% accuracy
	boolean firstRunFlag=true;
	int previousShapeFlag=0;
	float previousPitch;
	String previousNoteName;
	float previousProbability;
	// Coordinates and radius for all Mini Circles that represent the corresponding string
	int MINI_CIRCLE_RADIUS=4;
	int sixthStringX=182;
	int fifthStringX=191;
	int fourthStringX=202;
	int thirdStringX=392;
	int secondStringX=403;
	int firstStringX=411;
	int sixthANDfirstY=276;
	int fifthANDsecondY=209;
	int fourthANDthirdY=142;
	// Value of those Strings before we change tunings, set to STANDARD at runtime
	String tuningString1="E4";
	String tuningString2="B3";
	String tuningString3="G3";
	String tuningString4="D3";
	String tuningString5="A2";
	String tuningString6="E2";
	// Initialize Emboss Filters for the Shapes
	final EmbossMaskFilter T123embossFilter = new EmbossMaskFilter(new float[]{0f, -0.5f, 0.65f },0.85f,10f,8f);//cool bottom glow
	final EmbossMaskFilter CircleEmbossFilter = new EmbossMaskFilter(new float[]{0f, 0.16f, 0.45f },0.85f,30f,3.5f);
	final EmbossMaskFilter CLEFT4embossFilter = new EmbossMaskFilter(new float[]{-0.15f, -0.5f, 0.65f },0.85f,10f,8f);//cool bottom glow
	final EmbossMaskFilter CRIGHT4embossFilter = new EmbossMaskFilter(new float[]{0.15f, -0.5f, 0.65f },0.85f,10f,8f);//cool bottom glow

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		preparePaintbrushes();	// Calls FXN down below to set all Paint parameters for shapes, also sets emboss filters for shapes
		final DecimalFormat deciFormat= new DecimalFormat("#.##");  // trim least significant digits

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tarsos_dsp);
		if (savedInstanceState == null)
		{
			getSupportFragmentManager().beginTransaction().add(R.id.container, new PlaceholderFragment()).commit();
		}

		ActionBar bar = getActionBar();	//create action bar and change its color, font is changed below
		bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#04486F")));
		Typeface barFont= Typeface.createFromAsset(getAssets(), "limelight.ttf");
		SpannableStringBuilder spanString= new SpannableStringBuilder("OctaPitch");
		spanString.setSpan(new CustomTypefaceSpan("", barFont),0, spanString.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
		bar.setTitle(spanString);

		AudioDispatcher dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050,1024,0); //(samplerate,audiobuffersize,bufferoverlap)
		dispatcher.addAudioProcessor(new PitchProcessor(PitchEstimationAlgorithm.YIN, 22050, 1024, new PitchDetectionHandler()
		{
			@Override
			public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent)
			{
				final float pitchInHz = pitchDetectionResult.getPitch();
				final float prob = pitchDetectionResult.getProbability();
				runOnUiThread(new Runnable()
				{
					@Override
					public void run() 	//MAIN PROGRAM ENTRY
					{
						LinearLayout linlay = (LinearLayout) findViewById((R.id.triangle));//layout holds bitmap
						linlay.setBackground(new BitmapDrawable(bg));	//bitmap holds shapes

						TextView textPitch = (TextView) findViewById(R.id.textViewPitch);	//declares Recognized Pitch Textview
						TextView textNoteName = (TextView) findViewById(R.id.textViewNote);	//declares NoteName Textview
						TextView StandardIndicator= (TextView) findViewById(R.id.temp);	//declares an extra Textview
						textNoteName.setLayerType(View.LAYER_TYPE_SOFTWARE,null);// allow filter to be applied to our textView
						textPitch.setLayerType(View.LAYER_TYPE_SOFTWARE,null);	//  This turns OFF Hardware Acceleration on a View-level
						StandardIndicator.setLayerType(View.LAYER_TYPE_SOFTWARE,null);

						final EmbossMaskFilter textEmbossFilter = new EmbossMaskFilter(new float[]{0f, 0.3f, 0.3f },0.85f,3f,4f);
						textNoteName.getPaint().setMaskFilter(textEmbossFilter);	//set the textViews to textEmbossFilter above
						textPitch.getPaint().setMaskFilter(textEmbossFilter);
						StandardIndicator.getPaint().setMaskFilter(textEmbossFilter);

						Typeface Water_Font = Typeface.createFromAsset(getAssets(), "water2.ttf");	//set pitch and note to custom font
						textNoteName.setTypeface(Water_Font);
						textNoteName.setTextSize(50);
						textNoteName.setTextColor(Color.parseColor(PINK));
						textPitch.setTypeface(Water_Font);
						textPitch.setTextSize(30);
						textPitch.setTextColor(Color.parseColor(BLUE_DARK));

						if(pitchInHz==(-1)) //if we have no audio source then display --
						{
							textPitch.setText("");
							textNoteName.setText("--");
							//with no audio source, we have to turn all of the Tuning Indicators OFF
							paintMiniCircle.setColor(Color.parseColor(BG_COLOR));
							paintMiniCircle.setStyle(Paint.Style.FILL_AND_STROKE);
							canvas.drawCircle(sixthStringX,sixthANDfirstY,MINI_CIRCLE_RADIUS,paintMiniCircle);
							canvas.drawCircle(fifthStringX,fifthANDsecondY,MINI_CIRCLE_RADIUS,paintMiniCircle);
							canvas.drawCircle(fourthStringX,fourthANDthirdY,MINI_CIRCLE_RADIUS,paintMiniCircle);
							canvas.drawCircle(thirdStringX,fourthANDthirdY,MINI_CIRCLE_RADIUS,paintMiniCircle);
							canvas.drawCircle(secondStringX,fifthANDsecondY,MINI_CIRCLE_RADIUS,paintMiniCircle);
							canvas.drawCircle(firstStringX,sixthANDfirstY,MINI_CIRCLE_RADIUS,paintMiniCircle);
						}
// **** DRAW STARTS HERE ****
//     ** CIRCLE  **
						paintCircle.setColor(Color.parseColor(BLUE_LIGHT));
						paintCircle.setStyle(Paint.Style.STROKE);//this draws the Circle outline with color above
						canvas.drawCircle(300,37,30,paintCircle);//draw circle of radius 30 at pt 300,37
						paintCircle.setColor(Color.parseColor(BLUE_DARK));		//		( CENTER COLOR   )     (    EDGE COLOR        )
						paintCircle.setStyle(Paint.Style.FILL);//change to fill the circle instead of stroke or fill_and_stroke
						canvas.drawCircle(300,37,30,paintCircle);//draw circle of radius 30 at pt 300, y=37

//----- ROTATABLE TRIANGLES BELOW    ------
						Path L1path = new Path();	//create paths for all 8 triangles
						Path L2path = new Path();	//Side the triangle is on and number is 1-4, 1 is closest to middle, 4 is 90 degrees away
						Path L3path = new Path();
						Path L4path = new Path();
						Path R1path = new Path();
						Path R2path = new Path();
						Path R3path = new Path();
						Path R4path = new Path();

// THIS IS THE DECLARATION FOR ALL VARIABLES USED TO DRAW ANGLED TRIANGLES. INITIALIZED TO LEFT TRIANGLE #1
						paint.setColor(Color.parseColor(BLUE_LIGHT));
						paint.setStyle(Paint.Style.STROKE);
						float angle = (float) Math.toRadians(-22); // Angle to rotate, 0 degrees is pointing up, assuming base > height
						float height =72;//was 78
						float width =42;//was 45
						float centerX = 200;// Display coordinates where triangle will be drawn
						float centerY = 34;//used to be 30
						float x1 = centerX;// Vertex's coordinates before rotating
						float y1 = centerY - height / 2;
						float x2 = centerX + width / 2;
						float y2 = centerY + height / 2;
						float x3 = centerX - width / 2;
						float y3 = y2;
						float a0x = (float) ((x1 - centerX) * Math.cos(angle) - (y1 - centerY) * Math.sin(angle) + centerX);//3 points
						float a0y= (float) ((x1 - centerX) * Math.sin(angle) + (y1 - centerY) * Math.cos(angle) + centerY);
						float b0x = (float) ((x2 - centerX) * Math.cos(angle) - (y2 - centerY) * Math.sin(angle) + centerX);
						float b0y = (float) ((x2 - centerX) * Math.sin(angle) + (y2 - centerY) * Math.cos(angle) + centerY);
						float c0x= (float) ((x3 - centerX) * Math.cos(angle) - (y3 - centerY) * Math.sin(angle) + centerX);
						float c0y = (float) ((x3 - centerX) * Math.sin(angle) + (y3 - centerY) * Math.cos(angle) + centerY);
						L1path.moveTo(a0x, a0y);
						L1path.lineTo(b0x, b0y);
						L1path.lineTo(c0x,c0y);
						L1path.lineTo(a0x,a0y);
						L1path.lineTo(a0x,a0y);	//redundant call, but Path would not connect when used with CornerPathEffect(5<)
						canvas.drawPath(L1path, paint);

						// -----------------------     LEFT TRIANGLE #2  -----------------------
						angle = (float) Math.toRadians(-45);
						centerX = 142;
						centerY = 69;//used to be 65
						x1 = centerX;// Vertex's coordinates before rotating
						y1 = centerY - height / 2;
						x2 = centerX + width / 2;
						y2 = centerY + height / 2;
						x3 = centerX - width / 2;
						y3 = y2;
						a0x = (float) ((x1 - centerX) * Math.cos(angle) - (y1 - centerY) * Math.sin(angle) + centerX);//3 points
						a0y= (float) ((x1 - centerX) * Math.sin(angle) + (y1 - centerY) * Math.cos(angle) + centerY);
						b0x = (float) ((x2 - centerX) * Math.cos(angle) - (y2 - centerY) * Math.sin(angle) + centerX);
						b0y = (float) ((x2 - centerX) * Math.sin(angle) + (y2 - centerY) * Math.cos(angle) + centerY);
						c0x= (float) ((x3 - centerX) * Math.cos(angle) - (y3 - centerY) * Math.sin(angle) + centerX);
						c0y = (float) ((x3 - centerX) * Math.sin(angle) + (y3 - centerY) * Math.cos(angle) + centerY);
						L2path.moveTo(a0x, a0y);
						L2path.lineTo(b0x, b0y);
						L2path.lineTo(c0x,c0y);
						L2path.lineTo(a0x,a0y);
						L2path.lineTo(a0x,a0y);
						canvas.drawPath(L2path, paint);

						// -----------------------     LEFT TRIANGLE #3  -----------------------
						angle = (float) Math.toRadians(-67);
						centerX = 105;
						centerY = 119;//used to be 115
						x1 = centerX;// Vertex's coordinates before rotating
						y1 = centerY - height / 2;
						x2 = centerX + width / 2;
						y2 = centerY + height / 2;
						x3 = centerX - width / 2;
						y3 = y2;
						a0x = (float) ((x1 - centerX) * Math.cos(angle) - (y1 - centerY) * Math.sin(angle) + centerX);//3 points
						a0y= (float) ((x1 - centerX) * Math.sin(angle) + (y1 - centerY) * Math.cos(angle) + centerY);
						b0x = (float) ((x2 - centerX) * Math.cos(angle) - (y2 - centerY) * Math.sin(angle) + centerX);
						b0y = (float) ((x2 - centerX) * Math.sin(angle) + (y2 - centerY) * Math.cos(angle) + centerY);
						c0x= (float) ((x3 - centerX) * Math.cos(angle) - (y3 - centerY) * Math.sin(angle) + centerX);
						c0y = (float) ((x3 - centerX) * Math.sin(angle) + (y3 - centerY) * Math.cos(angle) + centerY);
						L3path.moveTo(a0x, a0y);
						L3path.lineTo(b0x, b0y);
						L3path.lineTo(c0x,c0y);
						L3path.lineTo(a0x,a0y);
						L3path.lineTo(a0x,a0y);
						canvas.drawPath(L3path, paint);

						// -----------------------     LEFT TRIANGLE #4  -----------------------
						angle = (float) Math.toRadians(-90);
						centerX = 90;
						centerY = 184;//used to be 180
						x1 = centerX;// Vertex's coordinates before rotating
						y1 = centerY - height / 2;
						x2 = centerX + width / 2;
						y2 = centerY + height / 2;
						x3 = centerX - width / 2;
						y3 = y2;
						a0x = (float) ((x1 - centerX) * Math.cos(angle) - (y1 - centerY) * Math.sin(angle) + centerX);//3 points
						a0y= (float) ((x1 - centerX) * Math.sin(angle) + (y1 - centerY) * Math.cos(angle) + centerY);
						b0x = (float) ((x2 - centerX) * Math.cos(angle) - (y2 - centerY) * Math.sin(angle) + centerX);
						b0y = (float) ((x2 - centerX) * Math.sin(angle) + (y2 - centerY) * Math.cos(angle) + centerY);
						c0x= (float) ((x3 - centerX) * Math.cos(angle) - (y3 - centerY) * Math.sin(angle) + centerX);
						c0y = (float) ((x3 - centerX) * Math.sin(angle) + (y3 - centerY) * Math.cos(angle) + centerY);
						L4path.moveTo(a0x, a0y);
						L4path.lineTo(b0x, b0y);
						L4path.lineTo(c0x,c0y);
						L4path.lineTo(a0x,a0y);
						L4path.lineTo(a0x,a0y);
						canvas.drawPath(L4path, paint);

						// -----------------------     RIGHT TRIANGLE #1  -----------------------
						angle = (float) Math.toRadians(22);
						centerX = 400;// Display coordinates where triangle will be drawn
						centerY = 34;//used to be 30
						x1 = centerX;// Vertex's coordinates before rotating
						y1 = centerY - height / 2;
						x2 = centerX + width / 2;
						y2 = centerY + height / 2;
						x3 = centerX - width / 2;
						y3 = y2;
						a0x = (float) ((x1 - centerX) * Math.cos(angle) - (y1 - centerY) * Math.sin(angle) + centerX);//3 points
						a0y= (float) ((x1 - centerX) * Math.sin(angle) + (y1 - centerY) * Math.cos(angle) + centerY);
						b0x = (float) ((x2 - centerX) * Math.cos(angle) - (y2 - centerY) * Math.sin(angle) + centerX);
						b0y = (float) ((x2 - centerX) * Math.sin(angle) + (y2 - centerY) * Math.cos(angle) + centerY);
						c0x= (float) ((x3 - centerX) * Math.cos(angle) - (y3 - centerY) * Math.sin(angle) + centerX);
						c0y = (float) ((x3 - centerX) * Math.sin(angle) + (y3 - centerY) * Math.cos(angle) + centerY);
						R1path.moveTo(a0x, a0y);
						R1path.lineTo(b0x, b0y);
						R1path.lineTo(c0x,c0y);
						R1path.lineTo(a0x,a0y);
						R1path.lineTo(a0x,a0y);
						canvas.drawPath(R1path, paint);

						// -----------------------     RIGHT TRIANGLE #2  -----------------------
						angle = (float) Math.toRadians(45);
						centerX = 460;// Display coordinates where triangle will be drawn
						centerY = 69;//used to be 65
						x1 = centerX;// Vertex's coordinates before rotating
						y1 = centerY - height / 2;
						x2 = centerX + width / 2;
						y2 = centerY + height / 2;
						x3 = centerX - width / 2;
						y3 = y2;
						a0x = (float) ((x1 - centerX) * Math.cos(angle) - (y1 - centerY) * Math.sin(angle) + centerX);//3 points
						a0y= (float) ((x1 - centerX) * Math.sin(angle) + (y1 - centerY) * Math.cos(angle) + centerY);
						b0x = (float) ((x2 - centerX) * Math.cos(angle) - (y2 - centerY) * Math.sin(angle) + centerX);
						b0y = (float) ((x2 - centerX) * Math.sin(angle) + (y2 - centerY) * Math.cos(angle) + centerY);
						c0x= (float) ((x3 - centerX) * Math.cos(angle) - (y3 - centerY) * Math.sin(angle) + centerX);
						c0y = (float) ((x3 - centerX) * Math.sin(angle) + (y3 - centerY) * Math.cos(angle) + centerY);
						R2path.moveTo(a0x, a0y);
						R2path.lineTo(b0x, b0y);
						R2path.lineTo(c0x,c0y);
						R2path.lineTo(a0x,a0y);
						R2path.lineTo(a0x,a0y);
						canvas.drawPath(R2path, paint);

						// -----------------------     RIGHT TRIANGLE #3  -----------------------
						angle = (float) Math.toRadians(67);
						centerX = 490;// Display coordinates where triangle will be drawn
						centerY = 119;//used to be 115
						x1 = centerX;// Vertex's coordinates before rotating
						y1 = centerY - height / 2;
						x2 = centerX + width / 2;
						y2 = centerY + height / 2;
						x3 = centerX - width / 2;
						y3 = y2;
						a0x = (float) ((x1 - centerX) * Math.cos(angle) - (y1 - centerY) * Math.sin(angle) + centerX);//3 points
						a0y= (float) ((x1 - centerX) * Math.sin(angle) + (y1 - centerY) * Math.cos(angle) + centerY);
						b0x = (float) ((x2 - centerX) * Math.cos(angle) - (y2 - centerY) * Math.sin(angle) + centerX);
						b0y = (float) ((x2 - centerX) * Math.sin(angle) + (y2 - centerY) * Math.cos(angle) + centerY);
						c0x= (float) ((x3 - centerX) * Math.cos(angle) - (y3 - centerY) * Math.sin(angle) + centerX);
						c0y = (float) ((x3 - centerX) * Math.sin(angle) + (y3 - centerY) * Math.cos(angle) + centerY);
						R3path.moveTo(a0x, a0y);
						R3path.lineTo(b0x, b0y);
						R3path.lineTo(c0x,c0y);
						R3path.lineTo(a0x,a0y);
						R3path.lineTo(a0x,a0y);
						canvas.drawPath(R3path, paint);

						// -----------------------     RIGHT TRIANGLE #4  -----------------------
						angle = (float) Math.toRadians(90);
						centerX = 500;// Display coordinates where triangle will be drawn
						centerY = 184;//used to be 180
						x1 = centerX;// Vertex's coordinates before rotating
						y1 = centerY - height / 2;
						x2 = centerX + width / 2;
						y2 = centerY + height / 2;
						x3 = centerX - width / 2;
						y3 = y2;
						a0x = (float) ((x1 - centerX) * Math.cos(angle) - (y1 - centerY) * Math.sin(angle) + centerX);//3 points
						a0y= (float) ((x1 - centerX) * Math.sin(angle) + (y1 - centerY) * Math.cos(angle) + centerY);
						b0x = (float) ((x2 - centerX) * Math.cos(angle) - (y2 - centerY) * Math.sin(angle) + centerX);
						b0y = (float) ((x2 - centerX) * Math.sin(angle) + (y2 - centerY) * Math.cos(angle) + centerY);
						c0x= (float) ((x3 - centerX) * Math.cos(angle) - (y3 - centerY) * Math.sin(angle) + centerX);
						c0y = (float) ((x3 - centerX) * Math.sin(angle) + (y3 - centerY) * Math.cos(angle) + centerY);
						R4path.moveTo(a0x, a0y);
						R4path.lineTo(b0x, b0y);
						R4path.lineTo(c0x,c0y);
						R4path.lineTo(a0x,a0y);
						R4path.lineTo(a0x,a0y);
						canvas.drawPath(R4path, paint);

//  -- SHADERS --
						Shader LT1yellowToPink= new LinearGradient(186,0,198,65, Color.parseColor(YELLOW_PALE), Color.parseColor(PINK), Shader.TileMode.CLAMP);
						Shader LT1blueToPink= new LinearGradient(186,0,198,65, Color.parseColor(BLUE_DARK), Color.parseColor(PINK), Shader.TileMode.CLAMP);
						Shader LT2yellowToPink= new LinearGradient(116,43,162,92, Color.parseColor(YELLOW_PALE), Color.parseColor(PINK), Shader.TileMode.CLAMP);
						Shader LT2blueToPink= new LinearGradient(116,43,162,92, Color.parseColor(BLUE_DARK), Color.parseColor(PINK), Shader.TileMode.CLAMP);
						Shader LT3yellowToPink= new LinearGradient(80,165,120,175, Color.parseColor(YELLOW_PALE), Color.parseColor(PINK), Shader.TileMode.CLAMP);
						Shader LT3blueToPink= new LinearGradient(80,165,120,175, Color.parseColor(BLUE_DARK), Color.parseColor(PINK), Shader.TileMode.CLAMP);
						Shader LT4yellowToPink= new LinearGradient(80,180,120,180, Color.parseColor(YELLOW_PALE), Color.parseColor(PINK), Shader.TileMode.CLAMP);
						Shader LT4blueToPink= new LinearGradient(80,180,120,180, Color.parseColor(BLUE_DARK), Color.parseColor(PINK), Shader.TileMode.CLAMP);
						Shader RT1yellowToPink= new LinearGradient(413,1,390,65, Color.parseColor(YELLOW_PALE), Color.parseColor(PINK), Shader.TileMode.CLAMP);
						Shader RT1blueToPink= new LinearGradient(413,1,390,65, Color.parseColor(BLUE_DARK), Color.parseColor(PINK), Shader.TileMode.CLAMP);
						Shader RT2yellowToPink= new LinearGradient(485,43,434,94, Color.parseColor(YELLOW_PALE), Color.parseColor(PINK), Shader.TileMode.CLAMP);
						Shader RT2blueToPink= new LinearGradient(485,43,434,94, Color.parseColor(BLUE_DARK), Color.parseColor(PINK), Shader.TileMode.CLAMP);
						Shader RT3yellowToPink= new LinearGradient(523,105,457,131, Color.parseColor(YELLOW_PALE), Color.parseColor(PINK), Shader.TileMode.CLAMP);
						Shader RT3blueToPink= new LinearGradient(523,105,457,131, Color.parseColor(BLUE_DARK), Color.parseColor(PINK), Shader.TileMode.CLAMP);
						Shader RT4yellowToPink= new LinearGradient(536,184,465,184, Color.parseColor(YELLOW_PALE), Color.parseColor(PINK), Shader.TileMode.CLAMP);
						Shader RT4blueToPink= new LinearGradient(536,184,465,184, Color.parseColor(BLUE_DARK), Color.parseColor(PINK), Shader.TileMode.CLAMP);

						paintLT1.setShader(LT1blueToPink);	//set shader to default; pink bottom with blue pointed tip
						paintLT2.setShader(LT2blueToPink);
						paintLT3.setShader(LT3blueToPink);
						paintLT4.setShader(LT4blueToPink);
						paintRT1.setShader(RT1blueToPink);
						paintRT2.setShader(RT2blueToPink);
						paintRT3.setShader(RT3blueToPink);
						paintRT4.setShader(RT4blueToPink);

						canvas.drawPath(L1path, paintLT1);	//draw all paths. This results in filled triangles using shaders above
						canvas.drawPath(L2path, paintLT2);
						canvas.drawPath(L3path, paintLT3);
						canvas.drawPath(L4path, paintLT4);
						canvas.drawPath(R1path, paintRT1);
						canvas.drawPath(R2path, paintRT2);
						canvas.drawPath(R3path, paintRT3);
						canvas.drawPath(R4path, paintRT4);

/* PROB GUI UPDATE */  if (prob>0.90)
						{
							textPitch.setText(String.valueOf(deciFormat.format(pitchInHz))+ "Hz");
							String NoteName=NoteFinder.Find(pitchInHz);	//passes pitch to Finder
							textNoteName.setText(NoteName);
							//make copies of these 3 displayed values. (For use when accuracy dips below 90%)
							previousPitch=pitchInHz;
							previousNoteName=NoteName;
							previousProbability=prob;

// ** TUNING MINI-CIRCLES DRAWN HERE **
							paintMiniCircle.setColor(Color.parseColor(BG_COLOR));
							paintMiniCircle.setStyle(Paint.Style.FILL_AND_STROKE);
							if(NoteName.equals(tuningString6))
							{
								paintMiniCircle.setColor(Color.parseColor(ORANGE));
							}
							canvas.drawCircle(sixthStringX,sixthANDfirstY,MINI_CIRCLE_RADIUS,paintMiniCircle);
							paintMiniCircle.setColor(Color.parseColor(BG_COLOR));
							if(NoteName.equals(tuningString5))
							{
								paintMiniCircle.setColor(Color.parseColor(ORANGE));
							}
							canvas.drawCircle(fifthStringX,fifthANDsecondY,MINI_CIRCLE_RADIUS,paintMiniCircle);
							paintMiniCircle.setColor(Color.parseColor(BG_COLOR));
							if(NoteName.equals(tuningString4))
							{
								paintMiniCircle.setColor(Color.parseColor(ORANGE));
							}
							canvas.drawCircle(fourthStringX,fourthANDthirdY,MINI_CIRCLE_RADIUS,paintMiniCircle);
							paintMiniCircle.setColor(Color.parseColor(BG_COLOR));
							if(NoteName.equals(tuningString3))
							{
								paintMiniCircle.setColor(Color.parseColor(ORANGE));
							}
							canvas.drawCircle(thirdStringX,fourthANDthirdY,MINI_CIRCLE_RADIUS,paintMiniCircle);
							paintMiniCircle.setColor(Color.parseColor(BG_COLOR));
							if(NoteName.equals(tuningString2))
							{
								paintMiniCircle.setColor(Color.parseColor(ORANGE));
							}
							canvas.drawCircle(secondStringX,fifthANDsecondY,MINI_CIRCLE_RADIUS,paintMiniCircle);
							paintMiniCircle.setColor(Color.parseColor(BG_COLOR));
							if(NoteName.equals(tuningString1))
							{
								paintMiniCircle.setColor(Color.parseColor(ORANGE));
							}
							canvas.drawCircle(firstStringX,sixthANDfirstY,MINI_CIRCLE_RADIUS,paintMiniCircle);
							paintMiniCircle.setColor(Color.parseColor(BG_COLOR));

//---  CHOOSE WHICH COLORS WILL BE USED TO FILL SHAPES  ---
							if(NoteFinder.LEFT_TRIANGLE1)
							{
								paintLT1.setShader(LT1yellowToPink);
								paintLT1.setColor(Color.parseColor(YELLOW_PALE));//pale yellow
								canvas.drawPath(L1path, paintLT1);
								previousShapeFlag=1;
							}
							if(NoteFinder.LEFT_TRIANGLE2)
							{
								paintLT2.setShader(LT2yellowToPink);
								paintLT2.setColor(Color.parseColor(YELLOW_PALE));//pale yellow
								canvas.drawPath(L2path, paintLT2);
								previousShapeFlag=2;
							}
							if(NoteFinder.LEFT_TRIANGLE3)
							{
								paintLT3.setShader(LT3yellowToPink);
								paintLT3.setColor(Color.parseColor(YELLOW_PALE));//pale yellow
								canvas.drawPath(L3path, paintLT3);
								previousShapeFlag=3;
							}
							if(NoteFinder.LEFT_TRIANGLE4)
							{
								paintLT4.setShader(LT4yellowToPink);
								paintLT4.setColor(Color.parseColor(YELLOW_PALE));//pale yellow
								canvas.drawPath(L4path, paintLT4);
								previousShapeFlag=4;
							}
							if(NoteFinder.RT_TRIANGLE1)
							{
								paintRT1.setShader(RT1yellowToPink);
								paintRT1.setColor(Color.parseColor(YELLOW_PALE));//pale yellow
								canvas.drawPath(R1path, paintRT1);
								previousShapeFlag=5;
							}
							if(NoteFinder.RT_TRIANGLE2)
							{
								paintRT2.setShader(RT2yellowToPink);
								paintRT2.setColor(Color.parseColor(YELLOW_PALE));//pale yellow
								canvas.drawPath(R2path, paintRT2);
								previousShapeFlag=6;
							}
							if(NoteFinder.RT_TRIANGLE3)
							{
								paintRT3.setShader(RT3yellowToPink);
								paintRT3.setColor(Color.parseColor(YELLOW_PALE));//pale yellow
								canvas.drawPath(R3path, paintRT3);
								previousShapeFlag=7;
							}
							if(NoteFinder.RT_TRIANGLE4)
							{
								paintRT4.setShader(RT4yellowToPink);
								paintRT4.setColor(Color.parseColor(YELLOW_PALE));//pale yellow
								canvas.drawPath(R4path, paintRT4);
								previousShapeFlag=8;
							}

							if(NoteFinder.A_PERFECT_CIRCLE)
							{
								paintCircle.setColor(Color.parseColor(PINK));		//		( CENTER COLOR   )     (    EDGE COLOR        )
								paintCircle.setStyle(Paint.Style.FILL);//change to fill the circle instead of stroke or fill_and_stroke
								canvas.drawCircle(300,37,30,paintCircle);//draw cricle of radius 30 at pt 300, y=37
								previousShapeFlag=9;
							}

							//DONE, set to false and return thread
							NoteFinder.LEFT_TRIANGLE1=false;
							NoteFinder.LEFT_TRIANGLE2=false;
							NoteFinder.LEFT_TRIANGLE3=false;
							NoteFinder.LEFT_TRIANGLE4=false;
							NoteFinder.RT_TRIANGLE1=false;
							NoteFinder.RT_TRIANGLE2=false;
							NoteFinder.RT_TRIANGLE3=false;
							NoteFinder.RT_TRIANGLE4=false;
							NoteFinder.A_PERFECT_CIRCLE=false;
							firstRunFlag=false;
						}  //end of probability IF

					else if( (!firstRunFlag) && (pitchInHz!=-1) )
					{
// If accuracy was UNDER 90%, and this was not the initial execution and there is valid audio, we ignore it and display previous value
// ---  COLOR TRIANGLE OF THE LAST SUCCESSFUL PITCH READ ABOVE 90%  ---
						switch (previousShapeFlag) {
							case 0:
								break;
							case 1:
								paintLT1.setShader(LT1yellowToPink);
								paintLT1.setColor(Color.parseColor(YELLOW_PALE));
								canvas.drawPath(L1path, paintLT1);
								break;
							case 2:
								paintLT2.setShader(LT2yellowToPink);
								paintLT2.setColor(Color.parseColor(YELLOW_PALE));
								canvas.drawPath(L2path, paintLT2);
								break;
							case 3:
								paintLT3.setShader(LT3yellowToPink);
								paintLT3.setColor(Color.parseColor(YELLOW_PALE));
								canvas.drawPath(L3path, paintLT3);
								break;
							case 4:
								paintLT4.setShader(LT4yellowToPink);
								paintLT4.setColor(Color.parseColor(YELLOW_PALE));
								canvas.drawPath(L4path, paintLT4);
								break;
							case 5:
								paintRT1.setShader(RT1yellowToPink);
								paintRT1.setColor(Color.parseColor(YELLOW_PALE));
								canvas.drawPath(R1path, paintRT1);
								break;
							case 6:
								paintRT2.setShader(RT2yellowToPink);
								paintRT2.setColor(Color.parseColor(YELLOW_PALE));
								canvas.drawPath(R2path, paintRT2);
								break;
							case 7:
								paintRT3.setShader(RT3yellowToPink);
								paintRT3.setColor(Color.parseColor(YELLOW_PALE));
								canvas.drawPath(R3path, paintRT3);
								break;
							case 8:
								paintRT4.setShader(RT4yellowToPink);
								paintRT4.setColor(Color.parseColor(YELLOW_PALE));
								canvas.drawPath(R4path, paintRT4);
								break;
							case 9:
								paintCircle.setColor(Color.parseColor(PINK));
								paintCircle.setStyle(Paint.Style.FILL);
								canvas.drawCircle(300,37,30,paintCircle);
								break;
							default:
								break;
						}
						//Set all displays to reflect the last reading of 90% accuracy or more. Prevents ON/OFF flickering
						textPitch.setText(String.valueOf(deciFormat.format(previousPitch)+ "Hz"));
						textNoteName.setText(previousNoteName);
					} //end of Else for Note Skipper
					}//end Runnable
				});	//END UIThread
			}//end handlePitch
		}));	//end 'new' PitchHandler on dispatch
		new Thread(dispatcher,"Audio Dispatcher").start();
		//new Thread(dispatcher2,"Audio Dispatcher").start();
	}	//end OnCreate


	private void preparePaintbrushes()
	{
		int cornerPathEffect=17;	//OG IS 20
		//Paintbrush for ALL Shape Outlines
		paint.setStrokeWidth(4);	//set stroke width to 4 for everything I draw.
		paint.setDither(true);                    // set the dither to true
		paint.setStrokeJoin(Paint.Join.ROUND);    // set the join to round you want
		paint.setStrokeCap(Paint.Cap.ROUND);      // set the paint cap to round too
		paint.setPathEffect(new CornerPathEffect(cornerPathEffect) );   // set the path effect when they join
		paint.setAntiAlias(true);
		//Paintbrush for Circle
		paintCircle.setStrokeWidth(4);	//set stroke width to 4 for everything I draw.
		paintCircle.setDither(true);                    // set the dither to true
		paintCircle.setStrokeJoin(Paint.Join.ROUND);    // set the join to round you want
		paintCircle.setStrokeCap(Paint.Cap.ROUND);      // set the paint cap to round too
		paintCircle.setPathEffect(new CornerPathEffect(cornerPathEffect) );   // set the path effect when they join
		paintCircle.setAntiAlias(true);
		//Paintbrush for MINI CIRCLES
		paintMiniCircle.setStrokeWidth(4);	//set stroke width to 4 for everything I draw.
		paintMiniCircle.setDither(true);                    // set the dither to true
		paintMiniCircle.setStrokeJoin(Paint.Join.ROUND);    // set the join to round you want
		paintMiniCircle.setStrokeCap(Paint.Cap.ROUND);      // set the paint cap to round too
		paintMiniCircle.setPathEffect(new CornerPathEffect(cornerPathEffect) );   // set the path effect when they join
		paintMiniCircle.setAntiAlias(true);
		//Paintbrushes for Triangles LT1-4
		paintLT1.setStrokeWidth(4);	//set stroke width to 4 for everything I draw.
		paintLT1.setDither(true);                    // set the dither to true
		paintLT1.setStrokeJoin(Paint.Join.ROUND);    // set the join to round you want
		paintLT1.setStrokeCap(Paint.Cap.ROUND);      // set the paint cap to round too
		paintLT1.setPathEffect(new CornerPathEffect(cornerPathEffect) );   // set the path effect when they join
		paintLT1.setAntiAlias(true);

		paintLT2.setStrokeWidth(4);	//set stroke width to 4 for everything I draw.
		paintLT2.setDither(true);                    // set the dither to true
		paintLT2.setStrokeJoin(Paint.Join.ROUND);    // set the join to round you want
		paintLT2.setStrokeCap(Paint.Cap.ROUND);      // set the paint cap to round too
		paintLT2.setPathEffect(new CornerPathEffect(cornerPathEffect) );   // set the path effect when they join
		paintLT2.setAntiAlias(true);

		paintLT3.setStrokeWidth(4);	//set stroke width to 4 for everything I draw.
		paintLT3.setDither(true);                    // set the dither to true
		paintLT3.setStrokeJoin(Paint.Join.ROUND);    // set the join to round you want
		paintLT3.setStrokeCap(Paint.Cap.ROUND);      // set the paint cap to round too
		paintLT3.setPathEffect(new CornerPathEffect(cornerPathEffect) );   // set the path effect when they join
		paintLT3.setAntiAlias(true);

		paintLT4.setStrokeWidth(4);	//set stroke width to 4 for everything I draw.
		paintLT4.setDither(true);                    // set the dither to true
		paintLT4.setStrokeJoin(Paint.Join.ROUND);    // set the join to round you want
		paintLT4.setStrokeCap(Paint.Cap.ROUND);      // set the paint cap to round too
		paintLT4.setPathEffect(new CornerPathEffect(cornerPathEffect) );   // set the path effect when they join
		paintLT4.setAntiAlias(true);

		//Paintbrushes for Triangles RT1-4
		paintRT1.setStrokeWidth(4);	//set stroke width to 4 for everything I draw.
		paintRT1.setDither(true);                    // set the dither to true
		paintRT1.setStrokeJoin(Paint.Join.ROUND);    // set the join to round you want
		paintRT1.setStrokeCap(Paint.Cap.ROUND);      // set the paint cap to round too
		paintRT1.setPathEffect(new CornerPathEffect(cornerPathEffect) );   // set the path effect when they join
		paintRT1.setAntiAlias(true);

		paintRT2.setStrokeWidth(4);	//set stroke width to 4 for everything I draw.
		paintRT2.setDither(true);                    // set the dither to true
		paintRT2.setStrokeJoin(Paint.Join.ROUND);    // set the join to round you want
		paintRT2.setStrokeCap(Paint.Cap.ROUND);      // set the paint cap to round too
		paintRT2.setPathEffect(new CornerPathEffect(cornerPathEffect) );   // set the path effect when they join
		paintRT2.setAntiAlias(true);

		paintRT3.setStrokeWidth(4);	//set stroke width to 4 for everything I draw.
		paintRT3.setDither(true);                    // set the dither to true
		paintRT3.setStrokeJoin(Paint.Join.ROUND);    // set the join to round you want
		paintRT3.setStrokeCap(Paint.Cap.ROUND);      // set the paint cap to round too
		paintRT3.setPathEffect(new CornerPathEffect(cornerPathEffect) );   // set the path effect when they join
		paintRT3.setAntiAlias(true);

		paintRT4.setStrokeWidth(4);	//set stroke width to 4 for everything I draw.
		paintRT4.setDither(true);                    // set the dither to true
		paintRT4.setStrokeJoin(Paint.Join.ROUND);    // set the join to round you want
		paintRT4.setStrokeCap(Paint.Cap.ROUND);      // set the paint cap to round too
		paintRT4.setPathEffect(new CornerPathEffect(cornerPathEffect) );   // set the path effect when they join
		paintRT4.setAntiAlias(true);

		// set Emboss Filters to the shapes
		paint.setMaskFilter(T123embossFilter);
		paintLT1.setMaskFilter(T123embossFilter);
		paintLT2.setMaskFilter(T123embossFilter);
		paintLT3.setMaskFilter(T123embossFilter);
		paintLT4.setMaskFilter(CLEFT4embossFilter);
		paintRT1.setMaskFilter(T123embossFilter);
		paintRT2.setMaskFilter(T123embossFilter);
		paintRT3.setMaskFilter(T123embossFilter);
		paintRT4.setMaskFilter(CRIGHT4embossFilter);
		paintCircle.setMaskFilter(CircleEmbossFilter);
		paintMiniCircle.setMaskFilter(T123embossFilter);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.tarsos_ds, menu);	//populate the menu with two menu options
		Typeface WaterTitle_Font = Typeface.createFromAsset(getAssets(), "water.ttf");	//declare fonts
		Typeface WaterOptions_Font = Typeface.createFromAsset(getAssets(), "water2final.ttf");
		for (int i = 0; i < menu.size(); i++)
		{
			MenuItem mi = menu.getItem(i);
			SubMenu subMenu = mi.getSubMenu();
			if (subMenu != null && subMenu.size() > 0)
			{
				for (int j = 0; j < subMenu.size(); j++)
				{
					MenuItem subMenuItem = subMenu.getItem(j);	// for each submenu item set the font
					applyFontToMenuItem(subMenuItem, WaterOptions_Font);
				}
			}
			applyFontToMenuItem(mi, WaterTitle_Font);	// set font for the two options in the menu
		}
		return super.onCreateOptionsMenu(menu);
	}


	private void applyFontToMenuItem(MenuItem mi, Typeface font)	//called above, uses the CustomTypeface Class to change menu fonts
	{
		SpannableString mNewTitle = new SpannableString(mi.getTitle());
		mNewTitle.setSpan(new CustomTypefaceSpan("", font), 0, mNewTitle.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
		mi.setTitle(mNewTitle);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int selectedTuning= item.getItemId();	// Tunings are chosen and managed here
		switch (selectedTuning) {
			case R.id.tuningSTANDARD:
				tuningString6="E2";
				tuningString5="A2";
				tuningString4="D3";
				tuningString3="G3";
				tuningString2="B3";
				tuningString1="E4";
				Toast.makeText(getApplicationContext(),"Tuning Changed to Standard", Toast.LENGTH_SHORT).show();
				break;
			case R.id.tuningDROPD:
				tuningString6="D2";
				tuningString5="A2";
				tuningString4="D3";
				tuningString3="G3";
				tuningString2="B3";
				tuningString1="E4";
				Toast.makeText(getApplicationContext(),"Tuning Changed to Drop D", Toast.LENGTH_SHORT).show();
				break;
			case R.id.tuningDOWNWHOLE:
				tuningString6="D2";
				tuningString5="G2";
				tuningString4="C3";
				tuningString3="F3";
				tuningString2="A3";
				tuningString1="D4";
				Toast.makeText(getApplicationContext(),"Tuning Changed to Whole Step Down", Toast.LENGTH_SHORT).show();
				break;
			case R.id.tuningDOWNHALF:
				tuningString6="D#2";
				tuningString5="G#2";
				tuningString4="C#3";
				tuningString3="F#3";
				tuningString2="A#3";
				tuningString1="D#4";
				Toast.makeText(getApplicationContext(),"Tuning Changed to Half Step Down", Toast.LENGTH_SHORT).show();
				break;
			case R.id.tuningDOUBLEDROPD:
				tuningString6="D2";
				tuningString5="A2";
				tuningString4="D3";
				tuningString3="G3";
				tuningString2="B3";
				tuningString1="D4";
				Toast.makeText(getApplicationContext(),"Tuning Changed to Double Drop D", Toast.LENGTH_SHORT).show();
				break;
			case R.id.tuningOPENC:
				tuningString6="C2";
				tuningString5="G2";
				tuningString4="C3";
				tuningString3="G3";
				tuningString2="C4";
				tuningString1="E4";
				Toast.makeText(getApplicationContext(),"Tuning Changed to Open C", Toast.LENGTH_SHORT).show();
				break;
			case R.id.tuningCELTIC:
				tuningString6="D2";
				tuningString5="A2";
				tuningString4="D3";
				tuningString3="G3";
				tuningString2="A3";
				tuningString1="D4";
				Toast.makeText(getApplicationContext(),"Tuning Changed to Celtic", Toast.LENGTH_SHORT).show();
				break;
			default:
				break;
		}	//end of switch statement

		if(selectedTuning==R.id.action_about)	//if the About OctaPitch is pressed, create alertDialog
		{
			AlertDialog.Builder aboutDialogBuilder = new AlertDialog.Builder(OctaPitch_Main.this);	//build in context of activity
			Typeface TitleFont=Typeface.createFromAsset(getAssets(), "limelight.ttf");	//declare fonts for the About dialog
			Typeface WaterContent_Font = Typeface.createFromAsset(getAssets(), "water2final.ttf");

			SpannableString dialogTitleSpanString= new SpannableString("About OctaPitch:");	//creates string for title, line below applies font
			dialogTitleSpanString.setSpan(new CustomTypefaceSpan("",TitleFont),0, dialogTitleSpanString.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
			aboutDialogBuilder.setTitle(dialogTitleSpanString);

			SpannableString dialogContentSpanString= new SpannableString("* CreaTOr:  taylOr  PlambeCk\n  taylOr@ligHtHOuseCOmm.COm\n\n* CrediTs CaN be fOuNd in tHe dOCumeNTATiOn On tHe GOOGle Play STOre");
			dialogContentSpanString.setSpan(new CustomTypefaceSpan("",WaterContent_Font),0,126, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);	//apply content font

			aboutDialogBuilder.setMessage(dialogContentSpanString).setCancelable(true);	//set content field
			AlertDialog aboutDialog = aboutDialogBuilder.create();	//create the dialog box
			aboutDialog.show();	//show dialog
			aboutDialog.setCanceledOnTouchOutside(true);	//we can end the dialog box with the BACK button OR just by touching somewhere else
		}
		if(selectedTuning==R.id.action_insight)	//if Insight is pressed, create alertDialog
		{
			AlertDialog.Builder insightDialogBuilder = new AlertDialog.Builder(OctaPitch_Main.this);	//build in context of activity
			Typeface TitleFont= Typeface.createFromAsset(getAssets(), "limelight.ttf");	//declare fonts for the About dialog
			Typeface California_Font = Typeface.createFromAsset(getAssets(), "california.TTF");
			Typeface CaliforniaIT_Font = Typeface.createFromAsset(getAssets(), "californiaitalic.TTF");

			SpannableString dialogTitleSpanString= new SpannableString("Insight:");	//creates string for title, line below applies font
			dialogTitleSpanString.setSpan(new CustomTypefaceSpan("",TitleFont),0, dialogTitleSpanString.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
			insightDialogBuilder.setTitle(dialogTitleSpanString);

			SpannableString dialogContentSpanString= new SpannableString
					("* The number on the display is the frequency heard by OctaPitch. " +
							"Above it is the closest note to that frequency. " +
							"Use the display to tune your guitar to that note. " +
							"When the note belongs to a string in the chosen tuning, an orange indicator appears next to the corresponding tuning peg. " +
							"Change Tunings in menu.\n" +
							"* There are no indicators for 7th or 8th strings, but those notes can still be heard and tuned by OctaPitch.\n" +
							"* OctaPitch is more accurate on acoustic and clean guitar sounds.\n" +
							"* Be sure to exit OctaPitch if you require the use of the microphone.\n\n"+
							"                ** Praise the Octopus **");

			dialogContentSpanString.setSpan(new CustomTypefaceSpan("",California_Font),0,553, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);	//apply content font
			dialogContentSpanString.setSpan(new CustomTypefaceSpan("",CaliforniaIT_Font),553,595, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);	//apply content font

			insightDialogBuilder.setMessage(dialogContentSpanString).setCancelable(true);	//set content field
			AlertDialog insightDialog = insightDialogBuilder.create();	//create the dialog box
			insightDialog.show();	//show dialog
			insightDialog.setCanceledOnTouchOutside(true);	//we can end the dialog box with the BACK button OR just by touching somewhere else
		}
		return super.onOptionsItemSelected(item);
	}  //end of OptionClicked Listener



	public static class PlaceholderFragment extends Fragment {
		public PlaceholderFragment()
		{
			//A placeholder fragment containing a simple view.
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
								 Bundle savedInstanceState)
		{
			//View rootView = inflater.inflate(R.layout.fragment_tarsos_ds,container, false);
			return inflater.inflate(R.layout.fragment_tarsos_ds,container, false);
		}
	}
}

