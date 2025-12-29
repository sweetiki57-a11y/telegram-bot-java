package com.example.telegrambot.commands;

import com.example.telegrambot.MyTelegramBot;

/**
 * Command for sending header
 */
public class SendHeaderCommand extends BaseCommand {
    
    public SendHeaderCommand(MyTelegramBot bot) {
        super(bot);
    }
    
    @Override
    public void execute(long chatId) {
        String text = "👽\n\n" +
                "https://f1.tf/Inoplaneteane website 👽\n\n" +
                "@BLSH7 @BLSH7Bot 🍫☘❄️🥥🔮🍭💎\n" +
                "====================\n" +
                "@Fredo_MarketMD \n" +
                "@MarketMD_FSBOT\n" +
                "@FSMD_RC 🍫☘🥥\n" +
                "====================\n" +
                "@A4R4M @A4R4Mbot 🍫☘🥥\n" +
                "====================\n" +
                "@BOBFOREVERTRUST\n" +
                "@BoboTrustForever_bot 🍫☘🥥\n" +
                "====================\n" +
                "@KandidatMD 🌲🍫🥥\n" +
                "====================\n" +
                "@PortMNC_RMD\n" +
                "@MonacoMD_BOT 🍫☘🥥\n" +
                "====================\n" +
                "@MARASLTMD @MS13MDbot 🍫☘❄️🥥🔮🍭💎\n" +
                "====================\n" +
                "@ZVDMD @MDZVDbot 🍫☘🥥\n" +
                "====================\n" +
                "@freshdr_777 @Fresh_dr777_bot 🍫🥥\n" +
                "====================\n" +
                "@N4N6N8 @MDNASAbot 🍫☘🥥\n" +
                "====================\n" +
                "@KrystaL337MD\n" +
                "@KrystaLMD373bot \n" +
                " 🍫☘❄️🥥\n" +
                "====================\n" +
                "@MrGreenNew☘🍫\n" +
                "@MRGRNBOT 👽\n" +
                "====================\n" +
                "@MarShmell09 @Mell09Bot 🍫❄️☘\n" +
                "====================\n" +
                "@Gr22nQueeN @queenstrbot 🍫❄️☘\n" +
                "====================\n" +
                "@ZoroTopZzZ\n" +
                "@ZoroTopZzZoperZzZ\n" +
                "@ZorroTopBot  ❄️🍫🍀\n" +
                "====================\n" +
                "@YO25SHOP 🍭\n" +
                "====================\n" +
                "@BELLUCCIMD 🍫🍀🥥\n" +
                "====================\n" +
                "@WWONCA @wwonca_bot🍫🍀🥥 \n" +
                "====================\n" +
                "@primeultra_bot \n" +
                "@SUPPRIME01 🍀\n" +
                "@SuperPrimeUltra 🍫🥥\n" +
                "====================\n" +
                "@DeiLmd @DeiLmd_bot 🍫🍀\n" +
                "====================\n" +
                "@mzpapa @moncler999bot\n" +
                "@mzreklama ❗️🥥 🌲🍫\n" +
                "====================\n" +
                "@smoky2bot 🍭\n" +
                "@smokymo_operator 🌲🍫\n" +
                "====================\n" +
                "@MARY_WEED 🥥🍫❄️💊💎\n" +
                "====================\n\n" +
                " 👽💰\n" +
                "@BLackCatEx \n" +
                "@TheMatrixEx \n" +
                "@CryptuLMDrsrv \n" +
                "@CandyEXC \n" +
                "@FRN_Crypto1 \n" +
                "@Monkeys_Crypto1  \n" +
                "@BTCBOSSMD  \n" +
                "@BLACKROCKEX \n" +
                "@HCHANGE1 \n" +
                "@Trust_LTC \n" +
                "@LTC_MAKLER \n" +
                "@StichLtc \n" +
                "@CryptoCOBA \n" +
                "@HiroshimaExc  \n" +
                "@PROFESOR_EX\n" +
                "@GoldXCHG\n" +
                "@mvp_exchange\n" +
                "@KryptoMahNEW\n" +
                "@GhostCryptoMD\n" +
                "@Lustig_LTC777\n" +
                "@ACHiLLES_LTC\n" +
                "@MIKE_LTC2\n" +
                "@LesbeaEX\n\n" +
                "@d3s1gngun 👨‍🎤👽 - design \n\n" +
                "https://f1.tf/Inoplaneteane website 👽";
        
        sendMessage(chatId, text);
    }
    
    @Override
    public String getDescription() {
        return "Show header with channels and exchangers";
    }
}
