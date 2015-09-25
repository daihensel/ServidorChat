/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service;

import bean.ChatMessage;
import bean.ChatMessage.Action;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Usuário
 */
public class servidorService {

    ServerSocket serverSocket;
    Socket socket;
    Map<String, ObjectOutputStream> mapOnlines = new HashMap<String, ObjectOutputStream>();

    public servidorService() {
        try {
            serverSocket = new ServerSocket(5555);
            System.out.println("Servidor on!");
            while (true) {
                socket = serverSocket.accept();

                new Thread(new ListenerSocket(socket)).start();
            }

        } catch (IOException ex) {
            Logger.getLogger(servidorService.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private class ListenerSocket implements Runnable {

        ObjectOutputStream output;
        ObjectInputStream input;

        public ListenerSocket(Socket socket) {
            try {
                this.output = new ObjectOutputStream(socket.getOutputStream());
                this.input = new ObjectInputStream(socket.getInputStream());
            } catch (IOException ex) {
                Logger.getLogger(servidorService.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        @Override
        public void run() {
            ChatMessage message = null;

            try {
                while ((message = (ChatMessage) input.readObject()) != null) {
                    Action action = message.getAction();

                    if (action.equals(Action.CONECT)) {
                        boolean isconnect = connect(message, output);
                        if (isconnect) {
                            mapOnlines.put(message.getNome(), output);
                            EnviaOnlines();
                        }
                    } else if (action.equals(Action.DISCONECT)) {
                        EnviaOnlines();
                        disconnect(message, output);
                        return;

                    } else if (action.equals(Action.SEND_ONE)) {
                        sendOne(message);
                    } else if (action.equals(Action.SEND_ALL)) {
                        sendAll(message);

                    }
                }
            } catch (IOException ex) {
                disconnect(message, output);
                EnviaOnlines();
                System.out.println(message.getNome() + " Deixou o chat");
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(servidorService.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

    }

    private boolean connect(ChatMessage message, ObjectOutputStream output) {
        if (mapOnlines.size() == 0) {
            message.setTexto("YES");
            send(message, output);

            return true;
        }
        for (Map.Entry<String, ObjectOutputStream> kv : mapOnlines.entrySet()) {
            if (kv.getKey().equals(message.getNome())) {
                message.setTexto("NO");
                send(message, output);
                return false;
            } else {
                message.setTexto("YES");
                send(message, output);
                return true;
            }
        }
        return false;
    }

    private void disconnect(ChatMessage message, ObjectOutputStream output) {
        mapOnlines.remove(message.getNome());

        message.setTexto("Saiu do chat");
        message.setAction(Action.SEND_ONE);

        sendAll(message);
        System.out.println("User " + message.getNome() + " saiu da sala");
    }

    private void send(ChatMessage message, ObjectOutputStream output) {

        try {
            output.writeObject(message);
        } catch (IOException ex) {
            Logger.getLogger(servidorService.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void sendOne(ChatMessage message) {
        for (Map.Entry<String, ObjectOutputStream> kv : mapOnlines.entrySet()) {
            if (kv.getKey().equals(message.getNomeReservado())) {

            }
            try {
                kv.getValue().writeObject(message);
            } catch (IOException ex) {
                Logger.getLogger(servidorService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void sendAll(ChatMessage message) {

        for (Map.Entry<String, ObjectOutputStream> kv : mapOnlines.entrySet()) {
            if (!kv.getKey().equals(message.getNome())) {
                message.setAction(Action.SEND_ONE);
                try {
                    kv.getValue().writeObject(message);
                } catch (IOException ex) {
                    Logger.getLogger(servidorService.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

    }

    private void EnviaOnlines() {
        Set<String> setNomes = new HashSet<String>();
        for (Map.Entry<String, ObjectOutputStream> kv : mapOnlines.entrySet()) {
            setNomes.add(kv.getKey());
        }

        ChatMessage message = new ChatMessage();
        message.setAction(Action.USERS_ONLINE);
        message.setSetOnlines(setNomes);
        for (Map.Entry<String, ObjectOutputStream> kv : mapOnlines.entrySet()) {
            message.setNome(kv.getKey());
            try {
                kv.getValue().writeObject(message);
            } catch (IOException ex) {
                Logger.getLogger(servidorService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
