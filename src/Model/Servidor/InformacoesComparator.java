package Model.Servidor;

import java.util.Comparator;

public class InformacoesComparator implements Comparator<Informacoes> {
    @Override
    public int compare(Informacoes o1, Informacoes o2) {
        return Integer.compare(o1.getLigacoes(),o2.getLigacoes());
    }
}
