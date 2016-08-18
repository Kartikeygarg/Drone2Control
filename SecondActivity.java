package com.example.defenselabs.dronecontrol;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;


public class SecondActivity extends AppCompatActivity implements View.OnTouchListener {

    ImageView drawringIV;
    Bitmap bitmap;
    double init_lat, init_long, init_height;
    double curr_lat, curr_long, curr_height;
    Button b1;
    Socket socket = null;
    int max_horizontal_angle = 70, max_vertical_angle = 35;
    boolean first_location = true;
    int image_width = 1199, image_height = 720;
    Bitmap newBitmap;
    float aa = 400, bb = 400;
    GestureDetector gdt;
    PrintStream PS;
    byte send_array[], input_array[];
    OutputStream out;
    String dstAddress;
    int dstPort;
    EditText editTextAddress, editTextPort;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        drawringIV = (ImageView) findViewById(R.id.drawringIV);
        b1 = (Button) findViewById(R.id.btn1);
        gdt = new GestureDetector(new GestureListener());
        editTextAddress = (EditText)findViewById(R.id.address);
        editTextAddress.setText("192.168.1.6");
        editTextPort = (EditText)findViewById(R.id.port);
        editTextPort.setText("4444");
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                SendReceiveAsyncTask obj = new SendReceiveAsyncTask(editTextAddress.getText().toString(),Integer.parseInt(editTextPort.getText().toString()));
                obj.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                drawringIV.setOnTouchListener(SecondActivity.this);
               /* Bitmap newBitmap = Bitmap.createBitmap(bitmap);
                Canvas canvas = new Canvas(newBitmap);
                Paint paint = new Paint();
                paint.setColor(Color.rgb(255, 0, 0));
                canvas.drawCircle(drawringIV.getWidth() / 2, (float) (drawringIV.getHeight() * 0.9), 20, paint);
                drawringIV.setImageBitmap(newBitmap);*/
            }
        });


    }

    /*@Override
    public void onDestroy() {
        super.onDestroy();

    }*/

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

    public float get_horizontal_disp(double latitude, double longitude) {
        double latitudeCircumference = 40075160 * Math.cos(init_lat * Math.PI / 180);
        double long_disp = (longitude - init_long) * latitudeCircumference / 360;
        double curr_depth = (latitude - init_lat) * 40008000 / 360;
        double max_disp = curr_depth * Math.tan(max_horizontal_angle);
        double disp = (long_disp / max_disp) * (drawringIV.getWidth() / 2);

        return (float) ((image_width / 2) + disp);
    }

    public float get_vertical_disp(double latitude, double altitude) {

        double vert_disp = (altitude - init_height);
        double curr_depth = (latitude - init_lat) * 40008000 / 360;
        double max_disp = curr_depth * Math.tan(max_vertical_angle);
        double disp = (vert_disp / max_disp) * (drawringIV.getHeight() * 0.9);

        return (float) ((image_height * 0.9) - disp);

    }

    public double getLatitudeOffset(double latitude, double offset) {
        double R = 6378137;
        double latitudeCircumference = 40075160 * Math.cos(init_lat * Math.PI / 180);
        double new_latitude = latitude + ((offset / R) * (180 / Math.PI));
        return new_latitude;
    }

    private double getLongitudeOffset(double longitude, double offset) {
        double R = 6378137;
        double latitudeCircumference = 40075160 * Math.cos(init_lat * Math.PI / 180);
        double new_long  = init_long + (offset *360 / latitudeCircumference);
        //double new_longitude = longitude + (offset / R) * (180 / Math.PI) / Math.cos(longitude * Math.PI / 180);
        return new_long;

    }

    /* private double getlongitudeoffset(double longitude, double offset) {
         double R = 6378137;
         double new_longitude = longitude + (offset/R) * ( 180 /Math.PI) / Math.cos(longitude * Math.PI/180);
         return new_longitude;

     }*/
    public void drawDrone(float horiz_disp, float vert_disp) {
        aa = horiz_disp;
        bb = vert_disp;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                newBitmap = Bitmap.createBitmap(bitmap);
                Canvas canvas = new Canvas(newBitmap);
                Paint paint = new Paint();
                paint.setColor(Color.rgb(255, 0, 0));
                canvas.drawCircle(aa, bb, 20, paint);
                drawringIV.setImageBitmap(newBitmap);
            }
        });

    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        gdt.onTouchEvent(motionEvent);
        return true;
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
                InputStream in = socket.getInputStream();
                out = socket.getOutputStream();
                PS = new PrintStream(socket.getOutputStream());
                BufferedReader is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PS.println("SEND");
                while (true) {
                    // String message = is.readLine();
                    send_array = new byte[1024];
                    input_array = new byte[1024];


                    IOUtils.read(in, input_array);
                    String message = new String(input_array, "UTF-8");

                    if (first_location) {
                        first_location = false;
                        drawDrone(image_width / 2, (float) (image_height * 0.9));
                        String a[] = message.split(":");
                        init_lat = Double.parseDouble(a[1]);
                        init_long = Double.parseDouble(a[3]);
                        init_height = Double.parseDouble(a[5]);
                        // drawDrone(get_horizontal_disp(init_lat,init_long),get_vertical_disp(init_lat,init_height));

                        // PS.println("SEND");

                    } else {
                        String a[] = message.split(":");
                        curr_lat = Double.parseDouble(a[1]);
                        curr_long = Double.parseDouble(a[3]);
                        curr_height = Double.parseDouble(a[5]);
                        if(curr_lat==init_lat && curr_long==init_long && curr_height==init_height)
                            drawDrone(image_width / 2, (float) (image_height * 0.9));

                        else
                            drawDrone(get_horizontal_disp(curr_lat, curr_long), get_vertical_disp(curr_lat, curr_height));
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        // PS.println("SEND");
                    }
                    System.arraycopy(toByteArray("SEND"),0,send_array,0,4);
                    out.write(send_array,0,send_array.length);

                }


            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

    }

    byte[] toByteArray(String value) {
        return new byte[] {
                (byte)(value.charAt(0) ),
                (byte)(value.charAt(1) ),
                (byte)(value.charAt(2)),
                (byte)value.charAt(3)   };
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent event) {
            float xPer = (event.getX() - (image_width / 2)) / (image_width / 2);
            float yPer = (float) (((image_height * 0.9) - event.getY()) / (image_height * 0.9));

            double curr_depth = (curr_lat - init_lat) * 40008000 / 360;
            double max_hori_disp = curr_depth * Math.tan(max_horizontal_angle);
            double max_vert_disp = curr_depth * Math.tan(max_vertical_angle);

            double new_hori_disp = max_hori_disp * xPer;
            double new_vert_disp = max_vert_disp * yPer;
            /*double latitudeCircumference = 40075160 * Math.cos(init_lat * Math.PI /180);
            double new_longitude = init_long + ((new_hori_disp *360)/ latitudeCircumference );*/

            final double new_longitude = getLongitudeOffset(init_long, new_hori_disp);
            //  double new_latitude = getLatitudeOffset( init_lat , new_hori_disp);
            final double new_height = init_height  + (yPer * max_vert_disp);
            SendAsyncTask obj = new SendAsyncTask(curr_lat, new_longitude, new_height);
            obj.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            return true;
        }
    }

    public class SendAsyncTask extends AsyncTask<Void, Void, Void> {

        double new_lat, new_long,new_height;

        SendAsyncTask(double new_lat,double new_long,double new_height) {
            this.new_height= new_height;
            this.new_lat= new_lat;
            this.new_long= new_long;
        }


        @Override
        protected Void doInBackground(Void... voids) {
            //   PS.println("Latitude:" + new_lat + ":Longitude:" + new_long + ":Height:" + new_height + ":\n");
            String str = "Latitude:" + new_lat + ":Longitude:" + new_long + ":Height:" + new_height + ":\n";
            byte dtn_array[] = new byte[1024];
            System.arraycopy(str.getBytes(),0,dtn_array,0,str.length());
            try {
                out.write(dtn_array,0,dtn_array.length);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Log.i("TAG","Latitude:" + new_lat + ":Longitude:" + new_long + ":Height:" + new_height + ":\n");
            return null;
        }
    }
}
