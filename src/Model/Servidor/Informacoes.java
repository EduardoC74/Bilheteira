package Model.Servidor;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

public class Informacoes implements Serializable {
    @Serial
    static final long serialVersionUID = 1L;
    private Integer porto;
    private String ip;
    private int ligacoes;
    private int versaoBd;
    private boolean disponivel;
    private String currentTime;
    private String dbName;
    private String msgAtualiza;
    private int portoUDPAtualiza;
    private int versaoBdAtualiza;


    ArrayList<String> msgSockett;
    public Informacoes(Integer porto, String ip, int ligacoes,String currentTime) {
        this.porto = porto;
        this.ip = ip;
        this.ligacoes = ligacoes;
        this.currentTime = currentTime;
    }

    public Informacoes(Integer porto, String ip, int ligacoes, String currentTime, int versaoBd, boolean disponivel) {
        this.porto = porto;
        this.ip = ip;
        this.ligacoes = ligacoes;
        this.currentTime = currentTime;
        this.versaoBd = versaoBd;
        this.disponivel = disponivel;
    }

    public Informacoes(Integer porto, String ip, int ligacoes) {
        this.porto = porto;
        this.ip = ip;
        this.ligacoes = ligacoes;
    }

    public ArrayList<String> getMsgSockett() {
        return msgSockett;
    }

    public void setMsgSockett(ArrayList<String> msgSockett) {
        this.msgSockett = msgSockett;
    }

    public int getVersaoBdAtualiza() {return versaoBdAtualiza;}
    public void setVersaoBdAtualiza(int versaoBdAtualiza) {this.versaoBdAtualiza = versaoBdAtualiza;}
    public int getPortoUDPAtualiza() {
        return portoUDPAtualiza;
    }

    public void setPortoUDPAtualiza(int portoUDPAtualiza) {
        this.portoUDPAtualiza = portoUDPAtualiza;
    }

    public String getMsgAtualiza() {
        return msgAtualiza;
    }

    public void setMsgAtualiza(String msgAtualiza) {
        this.msgAtualiza = msgAtualiza;
    }

    public Integer getPorto() {
        return porto;
    }

    public String getIp() {
        return ip;
    }
    public boolean isDisponivel() {
        return disponivel;
    }

    public void setDisponivel(boolean disponivel) {
        this.disponivel = disponivel;
    }
    public int getLigacoes() {
        return ligacoes;
    }
    public int getVersaoBd() {
        return versaoBd;
    }
    public void setVersaoBd(int versaoBd) {
        this.versaoBd = versaoBd;
    }
    public String getCurrentTime() {
        return currentTime;
    }
    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    @Override
    public String toString() {
        return "Porto:" + porto +" Ip:"+ ip +" LigacoesTCP:"+ ligacoes + " Hora:"+ currentTime + " versaoDB:" + versaoBd +
                " dbName:" + dbName + " disponivel:" + disponivel + " msgAtualiza: "+ msgAtualiza
                + " portoUDPAtualiza: " + portoUDPAtualiza +" versaoBdAtualiza: " + versaoBdAtualiza;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof Informacoes)) return false;
        Informacoes info = (Informacoes) o;
        return Objects.equals(porto, info.getPorto());
    }

    @Override
    public int hashCode() {
        return Objects.hash(porto);
    }

}
