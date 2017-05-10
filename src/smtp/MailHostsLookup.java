package smtp;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.naming.NamingException;
import java.util.Arrays;
import java.util.Comparator;

public class MailHostsLookup
{

    static String[] lookupMailHosts(String domainName) throws NamingException
    {
        // http://java.sun.com/j2se/1.5.0/docs/guide/jndi/jndi-dns.html
        //    - DNS Service Provider for the Java Naming Directory Interface (JNDI)

        InitialDirContext iDirC = new InitialDirContext();

        Attributes attributes = iDirC.getAttributes("dns:/" + domainName, new String[] {"MX"});

        Attribute attributeMX = attributes.get("MX");

        if (attributeMX == null)
        {
            return (new String[] {domainName});
        }


        String[][] pvhn = new String[attributeMX.size()][2];
        for (int i = 0; i < attributeMX.size(); i++)
        {
            pvhn[i] = ("" + attributeMX.get(i)).split("\\s+");
        }

        // sort the MX RRs by RR value (lower is preferred)
        Arrays.sort(pvhn, new Comparator<String[]>()
        {
            public int compare(String[] o1, String[] o2)
            {
                return (Integer.parseInt(o1[0]) - Integer.parseInt(o2[0]));
            }
        });

        // put sorted host names in an array, get rid of any trailing '.'
        String[] sortedHostNames = new String[pvhn.length];
        for (int i = 0; i < pvhn.length; i++)
        {
            sortedHostNames[i] = pvhn[i][1].endsWith(".") ?
                    pvhn[i][1].substring(0, pvhn[i][1].length() - 1) : pvhn[i][1];
        }
        return sortedHostNames;
    }
}