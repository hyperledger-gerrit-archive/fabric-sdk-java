This directory contains the default CA certificates used by HyperledgerFabric for signature validation.

These certificates are copied from the directories
 * hyperledger/fabric/msp/sampleconfig/admincerts
 * hyperledger/fabric/msp/sampleconfig/cacerts
 
The SDK loads these certificates into its CryptoPrimitives trust store and uses them when validating
signed messages from peer nodes.