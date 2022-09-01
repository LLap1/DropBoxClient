
package com.company;

public class Main {
    public static void main(String[] args) {
        DropBoxAccount account = new DropBoxAccount(
                "hizqkiq3952astb",
                "lavjuc69mbjuff7r",
                "50t7r3sy5ye19z1&vrqqzz5mgpzkm07",
                "Bearer oauth1.hizqkiq3952astb.50t7r3sy5ye19z1.dyhdndg46ks3ssy1.s0o3jl1it6ogu0e"
        );
        DropBoxClient client = new DropBoxClient(account);
        client.ListUserFiles();

    }
}
