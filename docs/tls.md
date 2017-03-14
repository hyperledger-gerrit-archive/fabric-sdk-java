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
   
   cfssl genkey clientcsr.json |cfssljson -bare client
   cfssl sign -ca ca.pem -ca-key ca-key.pem -csr client.csr | cfssljson -bare client
   
   ,,,,,,,
   ~/.GOPATH/bin/cfssl genkey orderer.cfssl.json | ~/.GOPATH/bin/cfssljson -bare ca
   
   
   xxxxxxxxxxxxxxxxxxx
   ~/.GOPATH/bin/cfssl genkey -initca ca.cfssl.json | ~/.GOPATH/bin/cfssljson -bare ca
   
   ~/.GOPATH/bin/cfssl genkey orderer.cfssl.json | ~/.GOPATH/bin/cfssljson -bare oderer
   ~/.GOPATH/bin/cfssl sign -ca ../ca/ca.pem -ca-key ../ca/ca-key.pem -csr oderer.csr  | ~/.GOPATH/bin/cfssljson -bare orderer
  
   ~/.GOPATH/bin/cfssl genkey  vp0.cfssl.json |  ~/.GOPATH/bin/cfssljson -bare vp0
   ~/.GOPATH/bin/cfssl sign -ca ../../ca/ca.pem -ca-key ../../ca/ca-key.pem -csr vp0.csr  | ~/.GOPATH/bin/cfssljson -bare  vp0
   
   
   