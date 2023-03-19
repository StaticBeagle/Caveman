#include "WSClient.hpp"

std::mutex WSClient::TransmitReceiveMutex;
std::condition_variable WSClient::TransmitReceiveConditionVariable;

bool WSClient::_dataReadyToSend = false;
bool _dataSent = false;
std::string WSClient::_serverMessage;
std::vector<mg_str> WSClient::_messageToSend;
std::string WSClient::_serverAddress;
bool WSClient::serverResponseReceivedOrTimedout = false;
bool WSClient::stopServer = false;
std::atomic<bool> WSClient::poll = false;
std::atomic<int> WSClient::connectionStatus = ConnectionStatus::Disconnected;
int numberOftimeOuts = 0;
bool WSClient::waiting = false;
int WSClient::timeOutSeconds = 10;
std::chrono::time_point<std::chrono::system_clock> WSClient::start;

std::mutex WSClient::_dataReadyToSendMutex;
std::condition_variable WSClient::_dataReadyToSendConditionVariable;

std::atomic<bool> WSClient::_isConnected = false;

WSClient::WSClient(unsigned int port)
	: WSClient("ws://127.0.0.1:", port)
{}

WSClient::WSClient(std::string serverAddress, unsigned int port)
	: _port{ port }
{
	WSClient::_serverAddress = serverAddress + std::to_string(port);
}

WSClient::~WSClient()
{
	this->stopServer = true;
	// need to wait for close confirmation from server thread
	// while(stillConnected) wait
	this->connectionStatus = ConnectionStatus::Disconnected;
	_dataReadyToSend = false;
	_dataSent = false;
	serverResponseReceivedOrTimedout = false;
}


bool WSClient::connect()
{
	if (!_isConnected)
	{
		this->stopServer = false;
		this->connectionStatus = ConnectionStatus::Disconnected;
		this->serverResponseReceivedOrTimedout = false;
		size_t(*callback)();
		callback = &WSClient::startWsClient;
		std::thread server_thread{ callback };
		server_thread.detach();
		WSClient::poll = true;
		// Spinning loop
		while (this->connectionStatus == ConnectionStatus::Disconnected);
	}
	return true;
}

void WSClient::pause()
{
	this->poll = false;
}

void WSClient::resume()
{
	this->poll = true;
}

bool WSClient::close()
{
	this->stopServer = true;
	return true;
}

bool WSClient::isConnected()
{
	return this->_isConnected;
}

void WSClient::sendMessage(char* message)
{
	this->sendMessage(std::vector<std::string>{ message });
}

void WSClient::sendMessage(const std::vector<std::string>& multiMessage)
{
	for (const auto& str : multiMessage)
	{
		this->_messageToSend.emplace_back(mg_str{ str.c_str(), str.length() });
	}

	//while (this->_dataReadyToSend);
	// Wait for data to be sent
	{
		std::unique_lock<std::mutex> lock(this->_dataReadyToSendMutex);
		this->_dataReadyToSend = true;
		this->_dataReadyToSendConditionVariable.wait(lock, [] { return !_dataReadyToSend; });
	}
	this->_messageToSend.swap(std::vector<mg_str>{});
}

unsigned int WSClient::getPort()
{
	return this->_port;
}

std::string&& WSClient::getServerMessage()
{
	return std::move(this->_serverMessage);
}

ConnectionStatus WSClient::getConnectionStatus()
{
	switch (this->connectionStatus)
	{
	case(Connected):
	{
		return ConnectionStatus::Connected;
	}
	case(Disconnected):
	{
		return ConnectionStatus::Disconnected;
	}
	case(TimeOut):
	{
		return ConnectionStatus::TimeOut;
	}
	case(ServerDown):
	{
		return ConnectionStatus::ServerDown;
	}
	case(InvalidAddress):
	{
		return ConnectionStatus::InvalidAddress;
	}
	case(Closed):
	{
		return ConnectionStatus::Closed;
	}
	default:
	{
		return ConnectionStatus::Closed;
	}
	}
}

void WSClient::ev_handler(struct mg_connection *nc, int ev, void *ev_data)
{
	struct websocket_message *wm = (struct websocket_message *) ev_data;
	(void)nc;

	switch (ev)
	{
	case MG_EV_CONNECT:
	{
		int status = *((int *)ev_data);
		if (status != 0)
		{
			connectionStatus = ConnectionStatus::ServerDown;
			poll = false;
			stopServer = true;
		}
		break;
	}
	case MG_EV_WEBSOCKET_HANDSHAKE_DONE:
	{
		connectionStatus = ConnectionStatus::Connected;
		_isConnected = true;
		break;
	}
	case MG_EV_POLL:
	{
		if (waiting)
		{
			std::chrono::duration<double> timeout = std::chrono::system_clock::now() - start;
			if (timeout.count() >= timeOutSeconds)
			{
				std::unique_lock<std::mutex> TxRxLock(TransmitReceiveMutex);
				connectionStatus = ConnectionStatus::TimeOut;
				++numberOftimeOuts;
				waiting = false;
				serverResponseReceivedOrTimedout = true;
				TxRxLock.unlock();
				TransmitReceiveConditionVariable.notify_one();
			}
		}
		break;
	}
	case MG_EV_WEBSOCKET_FRAME:
	{
		// TODO do we need atomic ints since we are putting the values inside mutex?
		std::unique_lock<std::mutex> TxRxLock(TransmitReceiveMutex);
		if (connectionStatus == ConnectionStatus::TimeOut)
		{
			--numberOftimeOuts;
			if (numberOftimeOuts == 0)
			{
				connectionStatus = ConnectionStatus::Connected;
			}
			break;
		}
		poll = false;
		_serverMessage.assign(reinterpret_cast<char*>(wm->data), wm->size);
		poll = true;
		waiting = false;
		serverResponseReceivedOrTimedout = true;
		TxRxLock.unlock();
		TransmitReceiveConditionVariable.notify_one();
		break;
	}
	case MG_EV_CLOSE:
	{
		if (connectionStatus != ConnectionStatus::ServerDown)
		{
			std::unique_lock<std::mutex> TxRxLock(TransmitReceiveMutex);
			poll = false;
			connectionStatus = ConnectionStatus::Closed;
			waiting = false;
			serverResponseReceivedOrTimedout = true;
			_isConnected = false;
			stopServer = true;
			numberOftimeOuts = 0;
			TxRxLock.unlock();
			TransmitReceiveConditionVariable.notify_one();
		}
		break;
	}
	}
}

size_t WSClient::startWsClient()
{
	struct mg_mgr mgr;
	struct mg_connection *nc;
	const char *chat_server_url = _serverAddress.c_str();
	void(*callback)(struct mg_connection*, int, void*);
	callback = &WSClient::ev_handler;
	mg_mgr_init(&mgr, NULL);
	nc = mg_connect_ws(&mgr, callback, chat_server_url, "ws_chat", NULL);
	if (nc == NULL)
	{
		waiting = false;
		std::unique_lock<std::mutex> TxRxLock(TransmitReceiveMutex);
		connectionStatus = ConnectionStatus::InvalidAddress;
		poll = false;
		TxRxLock.unlock();
		TransmitReceiveConditionVariable.notify_one();
		return 1;
	}
	while (!stopServer)
	{
		if (poll)
		{
			mg_mgr_poll(&mgr, 1);
		}

		if (_dataReadyToSend)
		{
			std::unique_lock<std::mutex> lock(_dataReadyToSendMutex);
			mg_send_websocket_framev(nc, WEBSOCKET_OP_TEXT, &_messageToSend[0], _messageToSend.size());
			start = std::chrono::system_clock::now();
			waiting = true;
			_dataReadyToSend = false;
			lock.unlock();
			_dataReadyToSendConditionVariable.notify_one();
		}
	}
	mg_mgr_free(&mgr);
	// Doesn't hurt to reset
	// all these variables to false
	stopServer = false;
	poll = false;
	_isConnected = false;
	_dataReadyToSend = false;
	_dataSent = false;
	waiting = false;
	return 0;
}