#ifndef _WS_CLIENT_H_INCLUDED
#define _WS_CLIENT_H_INCLUDED

#include "../mongoose/mongoose.h"
#include <string>
#include <thread>
#include <mutex>
#include <atomic>
#include <vector>

enum ConnectionStatus
{
	Connected,
	Disconnected,
	TimeOut,
	ServerDown,
	InvalidAddress,
	Closed,
};

class WSClient
{
public:
	static std::mutex TransmitReceiveMutex;
	static std::condition_variable TransmitReceiveConditionVariable;
	//static char* serverMessage;
	static bool serverResponseReceivedOrTimedout;

	WSClient(unsigned int);
	WSClient(std::string, unsigned int);

	~WSClient();

	bool connect();

	bool close();

	void pause();
	void resume();

	bool isConnected();

	void sendMessage(char*);
	void sendMessage(const std::vector<std::string>&);

	unsigned int getPort();

	std::string&& getServerMessage();

	ConnectionStatus getConnectionStatus();

private:
	static std::string _serverAddress;
	static bool _dataReadyToSend;
	static bool dataSent;
	static std::vector<mg_str> _messageToSend;
	static std::atomic<bool> poll;
	static std::atomic<int> connectionStatus;

	static std::string _serverMessage;

	static std::atomic<bool> _isConnected;

	static std::mutex _dataReadyToSendMutex;
	static std::condition_variable _dataReadyToSendConditionVariable;

	static bool stopServer;
	unsigned int _port;

	static bool waiting;
	static int timeOutSeconds;
	static std::chrono::time_point<std::chrono::system_clock> start;
	static void ev_handler(struct mg_connection*, int ev, void*);

	static size_t startWsClient();
};

#endif // _WS_CLIENT_H_INCLUDED