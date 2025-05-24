const { onValueCreated } = require("firebase-functions/v2/database");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendMessageNotification = onValueCreated('/messages/{chatRoomId}/{messageId}', async (event) => {
  const snapshot = event.data;
  const context = event.params;
  const message = snapshot.val();

  // Check if the message format is valid and contains required fields
  if (!message || !message.receiverId || !message.senderId || !message.message || !message.timestamp) {
    console.log('Geçersiz mesaj formatı:', message);
    return null;
  }

  const receiverId = message.receiverId;
  const senderId = message.senderId;

  // Gönderen kişiye bildirim gönderme
  if (receiverId === senderId) {
    console.log(`Gönderen (${senderId}) ve alıcı aynı, bildirim gönderilmiyor.`);
    return null;
  }

  // Alıcının FCM tokenını al
  const tokenSnapshot = await admin.database().ref(`/tokens/${receiverId}`).once('value');
  const token = tokenSnapshot.val();

  if (!token) {
    console.log(`Alıcı ${receiverId} için token bulunamadı, bildirim gönderilmiyor.`);
    return null;
  }

  // Gönderen kullanıcının bilgilerini al
  const senderSnapshot = await admin.database().ref(`/users/${senderId}`).once('value');
  const senderData = senderSnapshot.val();
  const senderName = senderData ? senderData.username : "Bir kullanıcı";

  const messagePayload = {
    data: {
      action: 'CHECK_UNREAD_MESSAGES', // Eylem: Okunmamış mesajları kontrol et
      chatRoomId: context.chatRoomId,
      senderId: senderId,
      receiverId: receiverId,
      messageId: context.messageId,
      title: 'Yeni Mesaj',
      body: message.message, // Mesaj içeriği
      timestamp: message.timestamp.toString()
    }
  };

  // Token'a mesajı gönder
  console.log(`Gönderiliyor - Token: ${token}, Payload: ${JSON.stringify(messagePayload)}`);

  try {
    // Sadece alıcıya bildirim gönder
    const response = await admin.messaging().sendToDevice(token, messagePayload);
    console.log(`FCM data mesajı başarıyla gönderildi. Alıcı: ${receiverId}, Gönderen: ${senderId}`);
  } catch (error) {
    console.error('FCM data mesajı gönderme hatası:', error);
  }

  return null;
});

