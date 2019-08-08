package app.saikat.DIManagement.Exceptions;

public class NoArgsPostConstructException extends RuntimeException{

    private static final long serialVersionUID = -2793762234559117544L;

    public NoArgsPostConstructException(String cls) {
        super(String.format("No argument @PostConstruct method requred for %s class", cls));
    }
}