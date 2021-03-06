package rawHttpServer;

import exceptions.HttpRequestParingException;
import hotelapp.ThreadSafeHotelData;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Implements an http server using raw sockets
 */
public class RawSocketHotelServer {

    public static final int PORT = 8080;

    private Map<String, String> handlers; // maps each url path to the appropriate handler

    private volatile boolean isShutdown = false;

    public RawSocketHotelServer() {

        handlers = new HashMap<>();
        handlers.put("attractions", "rawHttpServer.handlers.AttractionsHandler");
        handlers.put("hotelInfo", "rawHttpServer.handlers.HotelHandler");
        handlers.put("reviews", "rawHttpServer.handlers.ReviewsHandler");

    }

    public void startServer(ThreadSafeHotelData hotelData) {
        final ExecutorService threads = Executors.newFixedThreadPool(4);
        Runnable serverTask = new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket welcomingSocket = new ServerSocket(PORT);
                    System.out.println("Waiting for clients to connect...");
                    while (!isShutdown) {
                        Socket clientSocket = welcomingSocket.accept();
                        System.out.print("A Client Connected: ");
                        threads.submit(new RequestWorker(clientSocket));
                    }
                    if (isShutdown) {
                        welcomingSocket.close();
                    }
                } catch (IOException e) {
                    System.err.println("Unable to process client request");
                    e.printStackTrace();
                } catch (Exception e) {
                    System.out.println(e);
                }

            }
        };
        Thread serverThread = new Thread(serverTask);
        serverThread.start();
    }


    private class RequestWorker implements Runnable {

        private final Socket clientSocket;

        RequestWorker(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {

            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter outPutPrintWriter = new PrintWriter(clientSocket.getOutputStream());
                while (!clientSocket.isClosed()) {
                    String httpRequestString = reader.readLine();
                    HttpRequest httpRequest = null;
                    try {
                        httpRequest = new HttpRequest(httpRequestString);
                    } catch (HttpRequestParingException e) {
                        outPutPrintWriter.print(e.getHttpResponse());
                        System.out.println(e);
                        outPutPrintWriter.flush();
                        outPutPrintWriter.close();
                        return;
                    }

                    if (!"GET".equals(httpRequest.getHttpCRUD())) {
                        // return 405 Method Not Allowed
                        outPutPrintWriter.print("HTTP/1.1 405 Method Not Allowed\n");
                        System.out.println("Request fail: 405 Method Not Allowed: " + httpRequestString);
                        outPutPrintWriter.flush();
                        outPutPrintWriter.close();
                        return;
                    }

                    if (!handlers.containsKey(httpRequest.getAction())) {
                        outPutPrintWriter.print("HTTP/1.1 404 Page Not Found\n");
                        System.out.println("Request fail: 404 Page Not Found: " + httpRequestString);
                        outPutPrintWriter.flush();
                        outPutPrintWriter.close();
                        return;
                    }

                    try {
                        Class c = Class.forName(handlers.get(httpRequest.getAction()));
                        HttpHandler httpHandler = (HttpHandler) c.newInstance();
                        outPutPrintWriter.print("HTTP/1.1 200 OK\nContent-Type: application/json; charset=UTF-8\nConnection: close\n\n");
                        httpHandler.processRequest(httpRequest, outPutPrintWriter);
                        outPutPrintWriter.println();
                        outPutPrintWriter.flush();
                        System.out.println("Request success: " + httpRequestString);
                        return;
                    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                        System.out.println(e);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            } finally {
                try {
                    if (clientSocket != null)
                        clientSocket.close();
                } catch (IOException e) {
                    System.out.println("Can't close the socket : " + e);
                }
            }
        }
    }

}
