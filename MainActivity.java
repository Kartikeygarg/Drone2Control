package com.example.defenselabs.dronecontrol;

import android.graphics.Bitmap;
        import android.graphics.Canvas;
        import android.graphics.Color;
        import android.graphics.Paint;
        import android.os.AsyncTask;
        import android.support.v7.app.AppCompatActivity;
        import android.os.Bundle;
        import android.util.Log;
        import android.view.View;
        import android.widget.Button;
        import android.widget.ImageView;

        import java.io.BufferedReader;
        import java.io.IOException;
        import java.io.InputStream;
        import java.io.InputStreamReader;
        import java.io.PrintStream;
        import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    ImageView drawringIV;
    Bitmap bitmap;
    double init_lat, init_long, init_height;
    Button b1;
    Socket socket = null;
    int max_horizontal_angle = 60, max_vertical_angle = 45;
    boolean first_location = true;
    int image_width =1199, image_height = 720;
    Bitmap newBitmap;
    float aa=400,bb=400;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        drawringIV = (ImageView) findViewById(R.id.drawringIV);
        b1 = (Button) findViewById(R.id.btn1);


        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                SendReceiveAsyncTask obj = new SendReceiveAsyncTask("10.0.1.39",4466);
                obj.execute();
               /* Bitmap newBitmap = Bitmap.createBitmap(bitmap);
                Canvas canvas = new Canvas(newBitmap);
                Paint paint = new Paint();
                paint.setColor(Color.rgb(255, 0, 0));
                canvas.drawCircle(drawringIV.getWidth() / 2, (float) (drawringIV.getHeight() * 0.9), 20, paint);
                drawringIV.setImageBitmap(newBitmap);*/
            }
        });






    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {

        image_width = drawringIV.getWidth();
        image_height = drawringIV.getHeight();

        bitmap = Bitmap.createBitmap((int) getWindowManager()
                .getDefaultDisplay().getWidth(), (int) getWindowManager()
                .getDefaultDisplay().getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setColor(Color.rgb(0, 0, 0));
        paint.setAlpha(80);
        paint.setStrokeWidth(2);
        Log.i("TAG", "width : " + drawringIV.getWidth() + " Height : " + drawringIV.getHeight());
        canvas.drawLine(0, (float) (drawringIV.getHeight() * 0.9), drawringIV.getWidth(), (float) (drawringIV.getHeight() * 0.9), paint);
        canvas.drawLine(drawringIV.getWidth() / 2, 0, drawringIV.getWidth() / 2, drawringIV.getHeight(), paint);
       /* paint.setColor(Color.rgb(255 , 0, 0));
        canvas.drawCircle(drawringIV.getWidth() /2, (float) (drawringIV.getHeight()*0.9), 20, paint);*/
        drawringIV.setImageBitmap(bitmap);
    }

    public double getlongitudeoffset(double longitude) {
        return longitude - init_long;
    }

    public double getlatitudeoffset(double latitude) {
        return latitude - init_lat;
    }

    public float get_horizontal_disp(double latitude ,double longitude) {
        double latitudeCircumference = 40075160 * Math.cos(init_lat * Math.PI /180);
        double long_disp = (longitude - init_long) * latitudeCircumference / 360;
        double curr_depth = (latitude - init_lat) * 40008000 / 360;
        double max_disp = curr_depth * Math.tan(max_horizontal_angle);
        double disp =  (long_disp/max_disp)* (drawringIV.getWidth()/2);

        return (float) ((image_width/2)+disp);
    }

    public float get_vertical_disp(double latitude, double altitude)
    {

        double vert_disp = (altitude - init_height);
        double curr_depth = (latitude - init_lat) * 40008000 / 360;
        double max_disp = curr_depth * Math.tan(max_vertical_angle);
        double disp =  (vert_disp/max_disp)* (drawringIV.getHeight()*0.9);

        return (float) ((image_height*0.9) - disp);

    }

    public void drawDrone(float horiz_disp , float vert_disp)
    {
        aa=horiz_disp;
        bb= vert_disp;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                newBitmap = Bitmap.createBitmap(bitmap);
                Canvas canvas = new Canvas(newBitmap);
                Paint paint = new Paint();
                paint.setColor(Color.rgb(255,0 , 0));
                canvas.drawCircle( aa,  bb, 20, paint);
                drawringIV.setImageBitmap(newBitmap);
            }
        });

    }



    public class SendReceiveAsyncTask extends AsyncTask<Void, Void, Void> {

        String dstAddress, cmd;
        int dstPort;

        SendReceiveAsyncTask(String addr, int port) {
            dstAddress = addr;
            dstPort = port;
            //this.cmd = cmd;

        }


        @Override
        protected Void doInBackground(Void... voids) {

            try {
                socket = new Socket(dstAddress, dstPort);
                //InputStream in = socket.getInputStream();
                PrintStream PS = new PrintStream(socket.getOutputStream());
                BufferedReader is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PS.println("SEND");
                while(true) {
                    String message = is.readLine();
                    if(first_location)
                    {
                        first_location = false;
                        drawDrone(image_width/2, (float) (image_height*0.9));
                        String a[] = message.split(":");
                        init_lat =Double.parseDouble(a[1]);
                        init_long =Double.parseDouble(a[3]);
                        init_height =Double.parseDouble(a[5]);
                        drawDrone(get_horizontal_disp(init_lat,init_long),get_vertical_disp(init_lat,init_height));
                        PS.println("SEND");

                    }
                    else
                    {
                        String a[] = message.split(":");
                        double cur_lat =Double.parseDouble(a[1]);
                        double cur_long=Double.parseDouble(a[3]);
                        double cur_height =Double.parseDouble(a[5]);
                        drawDrone(get_horizontal_disp(cur_lat,cur_long),get_vertical_disp(cur_lat,cur_height));
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        PS.println("SEND");
                    }

                }



            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

    }




}
