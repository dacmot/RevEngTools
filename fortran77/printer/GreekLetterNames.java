/*
 * Created on 27-Jun-2006
 */
package fortran77.printer;

import java.util.HashSet;
import java.util.Set;

public class GreekLetterNames
{
    private Set<String> symbols;
    
    public GreekLetterNames()
    {
        symbols = new HashSet<String>();
        symbols.add("alpha");
        symbols.add("beta");
        symbols.add("gamma");
        symbols.add("delta");
        symbols.add("epsilon");
        symbols.add("zeta");
        symbols.add("eta");
        symbols.add("theta");
        symbols.add("iota");
        symbols.add("kappa");
        symbols.add("lambda");
        symbols.add("mu");
        symbols.add("nu");
        symbols.add("xi");
        symbols.add("omicron");
        symbols.add("pi");
        symbols.add("rho");
        symbols.add("sigma");
        symbols.add("tau");
        symbols.add("upsilon");
        symbols.add("phi");
        symbols.add("chi");
        symbols.add("psi");
        symbols.add("omega");
    }
    
    public boolean isGreekLetter(String name)
    {
        return symbols.contains(name);
    }
}
