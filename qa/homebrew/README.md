# Original NES QA cartridge

`build_qa_rom.py` generates a small mapper-0 test cartridge from original code and graphics. It includes no commercial game data, BIOS, firmware, or external assets. Run it with Python 3; it creates `EmuUI_QA.nes` and a hash/provenance `manifest.json` beside the script. Generated binaries are deliberately not committed.

The player is a green arrow. D-pad moves it; holding A makes it red; Start centers it. A separate moving beacon confirms that frames are advancing. This supports smoke testing ROM import, touch input, pause/resume, and save-state restoration.

This source and generated cartridge are dedicated to the public domain under [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/). To the extent copyright applies and can be waived, you may copy, modify, and redistribute them without permission. No warranty is provided.

Technical register references: [PPU registers](https://www.nesdev.org/wiki/PPU_registers), [standard controller](https://www.nesdev.org/wiki/Standard_controller). These references were used for hardware behavior; no code or assets were copied.
