package org.hyperledger.fabric_ca.sdk;

import java.util.ArrayList;
import java.util.Collection;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.hyperledger.fabric.sdk.helper.Utils;
import org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException;

public class CSRInfo {
    private String cn;

    private Collection<Name> names = new ArrayList<Name>();

    public CSRInfo(String cn) throws InvalidArgumentException {
        if (Utils.isNullOrEmpty(cn)) {
            throw new InvalidArgumentException("Common Name (CN) is required for CSR");
        }

        this.cn = cn;
    }

    public String getCn() {
        return this.cn;
    }

    void setCn(String cn) {
       this.cn = cn;
    }

    public void setNames(Collection<Name> names) {
        this.names = names;
    }

    public Collection<Name> getNames() {
        return this.names;
    }

    public void addName(String c, String st, String l, String o) {
        Name name = new Name(c, st, l, o);
        this.names.add(name);
    }


    public X500Name generateSubject() {
        X500NameBuilder nameBuilder = new X500NameBuilder();

        if (!this.cn.isEmpty()) {
            nameBuilder.addRDN(BCStyle.CN, this.cn);
        }

        for (Name name : this.names) {
            if (!name.getO().isEmpty()) {
                nameBuilder.addRDN(BCStyle.O, name.getO());
            }
            if (!name.getL().isEmpty()) {
                nameBuilder.addRDN(BCStyle.L, name.getL());
            }
            if (!name.getSt().isEmpty()) {
                nameBuilder.addRDN(BCStyle.ST, name.getSt());
            }
            if (!name.getC().isEmpty()) {
                nameBuilder.addRDN(BCStyle.C, name.getC());
            }
        }

        return nameBuilder.build();
    }

    class Name {
        private String c = "";
        private String st = "";
        private String l = "";
        private String o = "";

        Name(String c, String st, String l, String o) {
            this.c = c;
            this.st = st;
            this.l = l;
            this.o = o;
        }

        public String getC() {
            return c;
        }

        public String getSt() {
            return st;
        }

        public String getL() {
            return l;
        }

        public String getO() {
            return o;
        }
    }
}
