// @ts-ignore this resolves correctly in both dev and prod builds even though IntelliJ is upset
import cloudformationYml from '../../../infra/resonant.yml?raw';

export const CLOUDFORMATION_TEMPLATE = cloudformationYml;