### TLS
#### ALPN
Starting up the Java application you need have the ALPN agent `-javaagent:../jetty-alpn-agent-2.0.1.jar`
 go get -u github.com/cloudflare/cfssl/cmd/cfssl
 go get -u github.com/cloudflare/cfssl/cmd/cfssljson
 
 `cfssl genkey -initca csr.json | cfssljson -bare ca`
 Sample json:  cfssl.json:
     
    
    ```
    {
            "CN": "admin",
            "key": {
                "algo": "ecdsa",
                "size": 256
            },
            "names": [
                {
                    "O": "Hyperledger Fabric",
                    "OU": "CA",
                    "L": "Raleigh",
                    "ST": "North Carolina",
                    "C": "US"
                }
            ]
    }
    ```