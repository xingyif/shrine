# Generate keystores for QA network.

Run generate-qa-certs.sh with a keystore password to generate keystores for a SHRINE network. It will generate a keystore for every node defined in `QA_HOSTS`. Every keystore will contain the following entries:
- A public certificate for the network CA
- A private, query-signing certificate that is signed by the network CA
- A public certificate that serves the node's HTTPS
- For the hub keystore, a public certificate entry for every downstream node
- For a downstream keystore, the hub's https public certificate

Additionally, the script can be run with the --https-root flag. If it is, then instead of the hub having a public certificate for every downstream node, and every downstream node having the hub's https public cert, every node will have the public entry of the root HTTPS certificate.
