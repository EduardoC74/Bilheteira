package Model.Cliente;

import Model.Servidor.Informacoes;
import utils.Msg;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientReceiveTCP extends Thread {
    ArrayList<Informacoes> listaServidores;
    Socket sClient;
    ClientData data;
    AtomicInteger confirmaUpdate;
    public ClientReceiveTCP(ArrayList<Informacoes> listaServidores, Socket sClient, ClientData data, AtomicInteger confirmaUpdate) {
        this.listaServidores = listaServidores;
        this.sClient = sClient;
        this.data = data;
        this.confirmaUpdate = confirmaUpdate;
    }

    @Override
    public void run() {
        Msg msgTCP;
        try {
            InputStream is = sClient.getInputStream();
            ObjectInputStream oisTCP = new ObjectInputStream(is);

            while (true) {

                Object msg = oisTCP.readObject();
                if (msg instanceof Msg) {
                    msgTCP = (Msg) msg;
                    if(msgTCP.getMsg()!=null){
                        System.out.println(msgTCP.getMsg());
                        if(msgTCP.getMsg().equals("\nLogin efetuado como admin com sucesso!")){
                            confirmaUpdate.getAndSet(2);
                        }
                        else if(msgTCP.getMsg().equals("\nLogin efetuado com sucesso!")){
                            confirmaUpdate.getAndSet(1);
                        }
                        else if(msgTCP.getMsg().equals("\nEspetáculo Inexistente!") || msgTCP.getMsg().equals("\nNão é possivel selecionar este espetáculo!")){
                            confirmaUpdate.getAndSet(3);
                        }
                        else if(msgTCP.getMsg().contains("Lugar reservado")){
                            confirmaUpdate.getAndSet(4);
                        }
                    }else{
                        if(msgTCP.getIndex() == 0) //se for o primeiro, volta a ordenar
                            listaServidores.clear();

                        Informacoes info1 = new Informacoes(msgTCP.getPortoServer(), msgTCP.getIp(), msgTCP.getLigacoesTCP());
                        if (!listaServidores.contains(info1)) //se nao tiver na lista
                            listaServidores.add(info1);
                        else //se ja tiver mas for disponivel
                            listaServidores.set(listaServidores.lastIndexOf(info1), info1);

                        System.out.println("\nRecebi lista de Servidores Atualizada!");
                    }

                }

            }
        } catch (UnknownHostException | ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            if(!data.connectaTCPServidor(confirmaUpdate)) {
                throw new RuntimeException("ClientReceiveTCP Encerrou...");
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

