"""
This script automates the process of compiling, distributing, and managing the execution of a client-server application across multiple machines. It is designed for a specific project setup involving a map-reduce implementation. The script performs the following actions:

1. Reads a list of machine names from a specified file. These machines are the target hosts for deploying the server component of the application.
2. Compiles both the client and server components of the application using Maven.
3. Distributes the compiled JAR files to the first machine listed in the machines file. (Note: Current implementation limits distribution to the first machine only.)
4. Manages the server process on the target machines by first killing any existing instances and then starting the server.
5. Optionally, if an argument is passed to the script, it will only perform the process killing action without compiling or distributing the application.

Requirements:
- Python 3.x
- Maven installed and configured on the local machine.
- Access to the target machines via SSH, with password-based authentication configured.

Usage:
- Update the `machines_file_path` variable with the path to your machines list file.
- Ensure the directories for the client and server components are correctly set in the script.
- Run the script without arguments to compile, distribute, and start the server on the target machines.
- Run the script with any argument (e.g., `python compileAndDistribute.py kill`) to only kill the server processes on the target machines.

Important:
- The script currently uses hardcoded paths and credentials, which should be replaced with your specific configuration.
- Distribution is limited to the first machine in the list. This behavior can be modified as needed.
"""

import os
import subprocess
import sys
import getpass
import os
import paramiko
from paramiko import SSHClient
from scp import SCPClient

login = os.getenv('LOGIN')
password = os.getenv('PASSWORD')
machines_file_path = os.getenv('MACHINES_FILE_PATH')

# Read the machine names from the file
with open(machines_file_path, 'r') as file:
    machines = [line.strip() for line in file]

# This is the master machine

master = "tp-m6-17"
client_dir = os.getenv('CLIENT_DIR')
server_dir = os.getenv('SERVER_DIR')
client_target_dir = os.getenv('CLIENT_TARGET_DIR')
server_target_dir = os.getenv('SERVER_TARGET_DIR')
server_list_dir = os.getenv('SERVER_LIST_DIR')

username = "USERNAME"

# if argument is passed, only kill porocesses, otherwise compile and distribute

if len(sys.argv) > 1:
    for machine in machines:
        print(f"Killing process on {machine}")
        command = f'echo y | plink -pw {password} {login}@{machine} pkill -f /cal/exterieurs/{login}/slr207/MainMyftpserver-1-jar-with-dependencies.jar'
        subprocess.run(command, shell=True)
    sys.exit(0)    
else:    
    # Run the commands for the client
    os.chdir(client_dir)
    print(f"Current directory: {os.getcwd()}")
    subprocess.run(["mvn", "clean", "compile", "assembly:single"], check=True, shell=True)

    print("Client compiled")

    # Run the commands for the server
    os.chdir(server_dir)
    print(f"Current directory: {os.getcwd()}")
    subprocess.run(["mvn", "clean", "compile", "assembly:single"], check=True, shell=True)

    print("Server compiled")

    # send server code .jar to machines
    os.chdir(server_target_dir)

    # Use the password in the scp command
    command = f'echo y | pscp -pw {password} MainMyftpserver-1-jar-with-dependencies.jar {login}@{machines[0]}:/cal/exterieurs/{login}/slr207'
    subprocess.run(command, check=True, shell=True)
    
    # send client code .jar to machines
    os.chdir(client_target_dir)

    # Use the password in the scp command
    command = f'echo y | pscp -pw {password} MainClient-1-jar-with-dependencies.jar {login}@{machines[0]}:/cal/exterieurs/{login}/slr207'
    subprocess.run(command, check=True, shell=True)

    # send server list to remote Master
    os.chdir(server_list_dir)
    # Run server on machines
    
    # Use the password in the scp command
    command = f'echo y | pscp -pw {password} serverList.txt {login}@{machines[0]}:/cal/exterieurs/{login}/slr207'
    subprocess.run(command, check=True, shell=True)

    # First, kill the process if it is running


    for machine in machines:
        print(f"Killing process on {machine}")
        command = f'echo y | plink -pw {password} {login}@{machine} pkill -f /cal/exterieurs/{login}/slr207/MainMyftpserver-1-jar-with-dependencies.jar'
        subprocess.run(command, shell=True)

    # Then, start the process
    for machine in machines:
        print(f"Starting process on {machine}")
        command = f'echo y | plink -pw {password} {login}@{machine} "nohup java -jar /cal/exterieurs/{login}/slr207/MainMyftpserver-1-jar-with-dependencies.jar > /dev/null 2>&1 &"'
        subprocess.run(command, shell=True)

    # Run Master on the remote machine for 1, 2, ... 20 nodes
    
    for i in range(1, 4):        
   
        ssh = SSHClient()
        ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        ssh.connect(master, username=login, password=password)
        
        stdin, stdout, stderr = ssh.exec_command("cd {}; java -jar {} {}".format("/cal/exterieurs/{username}/slr207", "MainClient-1-jar-with-dependencies.jar", i))

        for line in stdout:
            print(line, end="")
        for line in stderr:
            print(line, end="")
            
    # Run the client from local machine  



    
    '''
    command = f'java -jar MainClient-1-jar-with-dependencies.jar'
    subprocess.run(command, shell=True)
    '''
    
    
