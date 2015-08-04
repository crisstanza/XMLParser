package parser;

import java.util.ArrayList;

public class Pontos extends ArrayList<Double> {

	private static final long serialVersionUID = 1L;

	private final String tipo;

	public Pontos(final String tipo) {
		this.tipo = tipo;
	}

	public String getTipo() {
		return tipo;
	}

}
